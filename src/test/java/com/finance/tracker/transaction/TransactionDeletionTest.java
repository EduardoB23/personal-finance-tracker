package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionDeletionTest extends IntegrationTest {

    private Profile profile;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        profile = profile("Maria");
        account = account("Corrente", "1000.00");
    }

    @Test
    void deletingExpenseRestoresBalance() throws Exception {
        Transaction expense = accountExpense(profile, account, "200.00", LocalDate.of(2026, 1, 10));
        BigDecimal before = balanceOf(account);

        mockMvc.perform(delete("/api/transactions/{id}", expense.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(jsonPath("$[*].id", not(hasItem(expense.getId().intValue()))));
        assertEquals(before.add(new BigDecimal("200.00")), balanceOf(account));
    }

    @Test
    void deletingIncomeRemovesItFromBalance() {
        Transaction income = transactionService.createTransaction(new TransactionRequest(TransactionType.INCOME,
                "Freela", category("Salário", CategoryType.INCOME).getId(), new BigDecimal("300.00"),
                LocalDate.of(2026, 1, 10), profile.getId(), account.getId(), null, false, null));
        BigDecimal before = balanceOf(account);

        transactionService.deleteTransaction(income.getId());

        assertEquals(before.subtract(new BigDecimal("300.00")), balanceOf(account));
    }

    @Test
    void deletingTransferRestoresBothAccounts() {
        BankAccount savings = account("Poupança", "0.00");
        Transaction transfer = transactionService.createTransaction(new TransactionRequest(TransactionType.TRANSFER,
                "Guardar", null, new BigDecimal("500.00"), LocalDate.of(2026, 1, 10), profile.getId(),
                account.getId(), savings.getId(), false, null));
        BigDecimal sourceBefore = balanceOf(account);
        BigDecimal targetBefore = balanceOf(savings);

        transactionService.deleteTransaction(transfer.getId());

        assertEquals(sourceBefore.add(new BigDecimal("500.00")), balanceOf(account));
        assertEquals(targetBefore.subtract(new BigDecimal("500.00")), balanceOf(savings));
    }

    @Test
    void deletingCardPurchaseRestoresInvoiceAndLimit() {
        CreditCard card = card("Nubank", "2000.00");
        Transaction purchase = cardExpense(profile, card, "150.00", LocalDate.of(2026, 1, 10), null);
        BigDecimal invoiceBefore = invoiceAmount(card, YearMonth.of(2026, 2));
        BigDecimal limitBefore = availableLimitOf(card);

        transactionService.deleteTransaction(purchase.getId());

        assertEquals(invoiceBefore.subtract(new BigDecimal("150.00")), invoiceAmount(card, YearMonth.of(2026, 2)));
        assertEquals(limitBefore.add(new BigDecimal("150.00")), availableLimitOf(card));
    }

    @Test
    void deletingOneInstallmentRemovesTheWholePurchase() {
        CreditCard card = card("Nubank", "2000.00");
        cardExpense(profile, card, "300.00", LocalDate.of(2026, 1, 10), 3);
        List<Transaction> installments = transactionService.getAllTransactions();
        assertEquals(3, installments.size());
        BigDecimal limitBefore = availableLimitOf(card);

        transactionService.deleteTransaction(installments.get(1).getId());

        assertTrue(transactionService.getAllTransactions().isEmpty());
        for (YearMonth month : List.of(YearMonth.of(2026, 2), YearMonth.of(2026, 3), YearMonth.of(2026, 4))) {
            assertEquals(new BigDecimal("0.00"), invoiceAmount(card, month));
        }
        assertEquals(limitBefore.add(new BigDecimal("300.00")), availableLimitOf(card));
    }

    @Test
    void deletedOccurrenceIsNotRecreated() {
        Transaction rule = rent();
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));
        Transaction february = occurrence(rule, LocalDate.of(2026, 2, 15));
        BigDecimal before = balanceOf(account);

        transactionService.deleteTransaction(february.getId());
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));

        assertEquals(before.add(new BigDecimal("900.00")), balanceOf(account));
        assertEquals(List.of(LocalDate.of(2026, 3, 15)), occurrencesOf(rule).stream().map(Transaction::getDate).toList());
    }

    @Test
    void deletingRuleStopsFutureOccurrences() {
        Transaction rule = rent();
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));
        List<Long> generated = occurrencesOf(rule).stream().map(Transaction::getId).toList();
        BigDecimal before = balanceOf(account);

        transactionService.deleteTransaction(rule.getId());
        entityManager.flush();
        entityManager.clear();
        transactionService.processRecurringTransactions(LocalDate.of(2026, 6, 15));

        assertEquals(before.add(new BigDecimal("900.00")), balanceOf(account));
        List<Transaction> remaining = transactionService.getAllTransactions();
        assertEquals(generated.stream().sorted().toList(), remaining.stream().map(Transaction::getId).sorted().toList());
    }

    @Test
    void deletingUnknownTransactionIsRejected() throws Exception {
        mockMvc.perform(delete("/api/transactions/{id}", 999_999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transação não encontrada"));
    }

    private Transaction rent() {
        return transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE, "Aluguel",
                expenseCategoryId("Moradia"), new BigDecimal("900.00"), LocalDate.of(2026, 1, 15), profile.getId(),
                account.getId(), null, true, null));
    }

    private List<Transaction> occurrencesOf(Transaction rule) {
        return transactionService.getAllTransactions().stream()
                .filter(transaction -> rule.getId().equals(transaction.getRecurrenceSourceId()))
                .toList();
    }

    private Transaction occurrence(Transaction rule, LocalDate date) {
        return occurrencesOf(rule).stream().filter(transaction -> transaction.getDate().equals(date)).findFirst().orElseThrow();
    }
}
