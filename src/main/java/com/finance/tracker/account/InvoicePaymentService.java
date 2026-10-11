package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class InvoicePaymentService {

    public record PaymentRequest(
            @NotNull(message = "Conta é obrigatória") Long accountId,
            @NotNull(message = "Perfil é obrigatório") Long profileId,
            @NotNull(message = "Valor é obrigatório") @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero") BigDecimal amount,
            @NotNull(message = "Data é obrigatória") LocalDate date
    ) {
    }

    public record PaymentView(Long id, LocalDate paidOn, Long accountId, Long profileId, BigDecimal amount) {
    }

    public record InvoiceView(YearMonth billingMonth, BigDecimal amount, BigDecimal paidAmount,
                              BigDecimal remainingAmount, LocalDate dueDate, String status, List<PaymentView> payments) {
    }

    private final CreditCardRepository cardRepository;
    private final CreditCardInvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final BankAccountRepository accountRepository;
    private final ProfileRepository profileRepository;

    public InvoicePaymentService(CreditCardRepository cardRepository, CreditCardInvoiceRepository invoiceRepository,
                                 InvoicePaymentRepository paymentRepository, BankAccountRepository accountRepository,
                                 ProfileRepository profileRepository) {
        this.cardRepository = cardRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<InvoiceView> invoicesOf(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new IllegalArgumentException("Cartão não encontrado");
        }
        return invoiceRepository.findByCreditCardIdOrderByBillingMonthAsc(cardId).stream()
                .map(invoice -> {
                    BigDecimal paid = paidOf(invoice);
                    BigDecimal remaining = invoice.getAmount().subtract(paid);
                    List<PaymentView> payments = paymentRepository.findByInvoiceIdOrderByPaidOnAscIdAsc(invoice.getId()).stream()
                            .map(p -> new PaymentView(p.getId(), p.getPaidOn(), p.getAccount().getId(),
                                    p.getProfile().getId(), p.getAmount()))
                            .toList();
                    return new InvoiceView(invoice.getBillingMonth(), invoice.getAmount(), paid, remaining,
                            invoice.getDueDate(), statusOf(paid, remaining), payments);
                })
                .toList();
    }

    @Transactional
    public InvoicePayment pay(Long cardId, YearMonth billingMonth, PaymentRequest request) {
        CreditCardInvoice invoice = invoiceRepository.findByCreditCardIdAndBillingMonth(cardId, billingMonth)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));
        BankAccount account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        BigDecimal remaining = invoice.getAmount().subtract(paidOf(invoice));
        if (request.amount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Valor maior que o saldo da fatura");
        }

        InvoicePayment payment = paymentRepository.save(new InvoicePayment(invoice, account, profile,
                request.amount(), request.date()));
        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);
        CreditCard card = invoice.getCreditCard();
        card.setAvailableLimit(card.getAvailableLimit().add(request.amount()));
        cardRepository.save(card);
        return payment;
    }

    // Estorno: devolve o valor à conta e tira do limite, como se o pagamento nunca tivesse existido
    @Transactional
    public void refund(Long paymentId) {
        InvoicePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));
        // Trava a fatura antes de mexer no pagamento, junto com pagamentos e compras da mesma fatura
        invoiceRepository.lockById(payment.getInvoice().getId());
        BigDecimal amount = payment.getAmount();
        BankAccount account = payment.getAccount();
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        CreditCard card = payment.getInvoice().getCreditCard();
        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        cardRepository.save(card);
        paymentRepository.delete(payment);
    }

    private BigDecimal paidOf(CreditCardInvoice invoice) {
        return paymentRepository.sumAmountByInvoiceId(invoice.getId()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String statusOf(BigDecimal paid, BigDecimal remaining) {
        if (paid.signum() == 0) return "Aberta";
        if (remaining.signum() == 0) return "Paga";
        return "Parcial";
    }
}
