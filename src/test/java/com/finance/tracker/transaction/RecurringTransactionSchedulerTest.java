package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringTransactionSchedulerTest extends IntegrationTest {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private Profile profile;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        profile = profile("Família Recorrência");
        account = account("Conta", "1000.00");
    }

    @Test
    void dueOccurrencesApplyTheirEffect() {
        Transaction rule = rent(LocalDate.of(2026, 1, 15));

        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));

        List<Transaction> occurrences = occurrencesOf(rule);
        assertEquals(List.of(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 15)),
                occurrences.stream().map(Transaction::getDate).sorted().toList());
        assertEquals(new BigDecimal("-1700.00"), balanceOf(account));
    }

    @Test
    void processingTwiceChangesNothing() {
        Transaction rule = rent(LocalDate.of(2026, 1, 15));
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));
        BigDecimal balanceAfterFirstRun = balanceOf(account);

        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));

        assertEquals(2, occurrencesOf(rule).size());
        assertEquals(balanceAfterFirstRun, balanceOf(account));
    }

    @Test
    void cardRecurrenceGoesToEachInvoice() {
        CreditCard card = card("Nubank", "2000.00");
        Transaction rule = transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE,
                "Streaming", expenseCategoryId("Lazer"), new BigDecimal("40.00"), LocalDate.of(2026, 1, 5),
                profile.getId(), null, null, true, null, card.getId()));

        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 5));

        assertEquals(1, occurrencesOf(rule).size());
        assertEquals(new BigDecimal("40.00"), invoiceAmount(card, YearMonth.of(2026, 2)));
        assertEquals(new BigDecimal("40.00"), invoiceAmount(card, YearMonth.of(2026, 3)));
        assertEquals(new BigDecimal("1920.00"), availableLimitOf(card));
    }

    @Test
    void futureOccurrencesAreNotCreated() {
        LocalDate today = LocalDate.now();
        Transaction rule = rent(today.minusMonths(1));

        transactionService.processRecurringTransactions(today.plusMonths(3));

        List<Transaction> occurrences = occurrencesOf(rule);
        assertEquals(1, occurrences.size());
        assertTrue(occurrences.stream().noneMatch(occurrence -> occurrence.getDate().isAfter(today)));
    }

    @Test
    void startupProcessesDueOccurrences() {
        LocalDate ruleDate = LocalDate.now().minusMonths(2);
        Transaction rule = rent(ruleDate);

        applicationContext.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0],
                applicationContext, Duration.ZERO));

        assertTrue(occurrencesOf(rule).stream()
                .anyMatch(occurrence -> occurrence.getDate().equals(ruleDate.plusMonths(1))));
    }

    private Transaction rent(LocalDate date) {
        return transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE, "Aluguel",
                expenseCategoryId("Moradia"), new BigDecimal("900.00"), date, profile.getId(), account.getId(),
                null, true, null));
    }

    private List<Transaction> occurrencesOf(Transaction rule) {
        return transactionService.getAllTransactions().stream()
                .filter(transaction -> rule.getId().equals(transaction.getRecurrenceSourceId()))
                .toList();
    }
}
