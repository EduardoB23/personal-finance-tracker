package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Sem transação de teste: cada pagamento precisa ser de uma transação própria, como em produção
@SpringBootTest
class InvoicePaymentConcurrencyTest {

    @Autowired private InvoicePaymentService paymentService;
    @Autowired private CreditCardInvoiceService invoiceService;
    @Autowired private CreditCardRepository cardRepository;
    @Autowired private CreditCardInvoiceRepository invoiceRepository;
    @Autowired private InvoicePaymentRepository paymentRepository;
    @Autowired private BankAccountRepository accountRepository;
    @Autowired private ProfileRepository profileRepository;

    private final List<Long> paymentIds = new ArrayList<>();
    private Long invoiceId;
    private Long cardId;
    private Long accountId;
    private Long profileId;

    @AfterEach
    void cleanUp() {
        paymentIds.forEach(paymentRepository::deleteById);
        if (invoiceId != null) invoiceRepository.deleteById(invoiceId);
        if (cardId != null) cardRepository.deleteById(cardId);
        if (accountId != null) accountRepository.deleteById(accountId);
        if (profileId != null) profileRepository.deleteById(profileId);
    }

    @Test
    void simultaneousFullPaymentsAcceptOnlyOne() throws Exception {
        Profile profile = profileRepository.save(new Profile("Concorrência"));
        profileId = profile.getId();
        BankAccount account = accountRepository.save(new BankAccount("Conta", AccountType.CHECKING, new BigDecimal("5000")));
        accountId = account.getId();
        CreditCard card = cardRepository.save(new CreditCard("Cartão", new BigDecimal("3000"), new BigDecimal("3000"), 20, 10));
        cardId = card.getId();
        CreditCardInvoice invoice = invoiceService.addPurchase(card, new BigDecimal("1000"), LocalDate.of(2026, 1, 10)) ;
        invoiceId = invoice.getId();
        YearMonth month = YearMonth.of(2026, 2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            InvoicePaymentService.PaymentRequest request = new InvoicePaymentService.PaymentRequest(
                    account.getId(), profile.getId(), new BigDecimal("1000"), LocalDate.of(2026, 2, 5));
            Callable<String> pay = () -> {
                try {
                    paymentIds.add(paymentService.pay(card.getId(), month, request).getId());
                    return "ok";
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }
            };
            List<Future<String>> results = pool.invokeAll(List.of(pay, pay));
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : results) outcomes.add(future.get());

            assertEquals(1, outcomes.stream().filter("ok"::equals).count(), outcomes.toString());
            assertEquals(1, outcomes.stream().filter("Valor maior que o saldo da fatura"::equals).count(), outcomes.toString());
        } finally {
            pool.shutdownNow();
        }
    }
}
