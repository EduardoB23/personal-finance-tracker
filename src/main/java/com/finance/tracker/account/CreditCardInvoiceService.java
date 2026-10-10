package com.finance.tracker.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class CreditCardInvoiceService {

    private final CreditCardInvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;

    public CreditCardInvoiceService(CreditCardInvoiceRepository invoiceRepository,
                                    CreditCardRepository creditCardRepository) {
        this.invoiceRepository = invoiceRepository;
        this.creditCardRepository = creditCardRepository;
    }

    @Transactional
    public CreditCardInvoice addPurchase(CreditCard card, BigDecimal amount, LocalDate purchaseDate) {
        YearMonth billingMonth = calculateBillingMonth(card, purchaseDate);
        CreditCardInvoice invoice = invoiceRepository.findByCreditCardIdAndBillingMonth(card.getId(), billingMonth)
                .orElseGet(() -> new CreditCardInvoice(card, billingMonth, BigDecimal.ZERO, dueDate(card, billingMonth)));
        invoice.setAmount(invoice.getAmount().add(amount));
        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        creditCardRepository.save(card);
        return invoiceRepository.save(invoice);
    }

    // Estorno de uma compra: tira o valor da fatura do mês e devolve o limite
    @Transactional
    public void removePurchase(CreditCard card, BigDecimal amount, LocalDate purchaseDate) {
        YearMonth billingMonth = calculateBillingMonth(card, purchaseDate);
        CreditCardInvoice invoice = invoiceRepository.findByCreditCardIdAndBillingMonth(card.getId(), billingMonth)
                .orElseThrow(() -> new IllegalStateException("Fatura da compra não encontrada"));
        invoice.setAmount(invoice.getAmount().subtract(amount));
        card.setAvailableLimit(card.getAvailableLimit().add(amount));
        creditCardRepository.save(card);
        invoiceRepository.save(invoice);
    }

    private YearMonth calculateBillingMonth(CreditCard card, LocalDate purchaseDate) {
        int monthsUntilDue = purchaseDate.getDayOfMonth() <= card.getClosingDay() ? 1 : 2;
        return YearMonth.from(purchaseDate).plusMonths(monthsUntilDue);
    }

    private LocalDate dueDate(CreditCard card, YearMonth billingMonth) {
        return billingMonth.atDay(Math.min(card.getDueDay(), billingMonth.lengthOfMonth()));
    }
}