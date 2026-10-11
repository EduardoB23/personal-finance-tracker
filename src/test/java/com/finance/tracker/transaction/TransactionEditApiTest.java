package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.finance.tracker.transaction.TransactionType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionEditApiTest extends IntegrationTest {

    private String body(Transaction original, Map<String, Object> overrides) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("type", original.getType().name());
        body.put("description", original.getDescription());
        body.put("categoryId", original.getCategoryId());
        body.put("amount", original.getAmount());
        body.put("date", original.getDate().toString());
        body.put("profileId", original.getProfileId());
        body.put("accountId", original.getAccountId());
        body.put("creditCardId", original.getCreditCardId());
        body.put("recurring", false);
        body.putAll(overrides);
        return json(body);
    }

    @Test
    void editAmountAdjustsAccountBalance() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        Transaction expense = accountExpense(maria, conjunta, "100.00", LocalDate.of(2026, 1, 5));
        assertEquals(0, new BigDecimal("4900").compareTo(balanceOf(conjunta)));

        mockMvc.perform(put("/api/transactions/" + expense.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(expense, Map.of("amount", "150.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150.0));

        assertEquals(0, new BigDecimal("4850").compareTo(balanceOf(conjunta)));
    }

    @Test
    void editDateMovesCardPurchaseToAnotherInvoice() throws Exception {
        Profile maria = profile("Maria");
        CreditCard nubank = card("Nubank", "3000");
        Transaction purchase = cardExpense(maria, nubank, "100.00", LocalDate.of(2026, 1, 10), 1);
        assertEquals(0, new BigDecimal("2900").compareTo(availableLimitOf(nubank)));

        mockMvc.perform(put("/api/transactions/" + purchase.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(purchase, Map.of("date", "2026-01-25", "creditCardId", nubank.getId()))))
                .andExpect(status().isOk());

        // Compra de 25/01 (depois do fechamento do dia 20) cai na fatura de março
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[?(@.billingMonth == '2026-03')].amount").value(org.hamcrest.Matchers.hasItem(100.0)));
        assertEquals(0, new BigDecimal("2900").compareTo(availableLimitOf(nubank)));
    }

    @Test
    void editingOneInstallmentChangesOnlyThatInstallment() throws Exception {
        Profile maria = profile("Maria");
        CreditCard nubank = card("Nubank", "3000");
        Transaction parcel = cardExpense(maria, nubank, "300.00", LocalDate.of(2026, 1, 10), 3);
        Transaction sibling = transactionService.getAllTransactions().stream()
                .filter(t -> parcel.getInstallmentGroupId().equals(t.getInstallmentGroupId()) && !t.getId().equals(parcel.getId()))
                .findFirst().orElseThrow();
        BigDecimal siblingAmount = sibling.getAmount();

        mockMvc.perform(put("/api/transactions/" + parcel.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(parcel, Map.of("amount", "120.00"))))
                .andExpect(status().isOk());

        assertEquals(0, siblingAmount.compareTo(transactionService.getAllTransactions().stream()
                .filter(t -> t.getId().equals(sibling.getId())).findFirst().orElseThrow().getAmount()));
    }

    @Test
    void editingRecurringRuleIsRefused() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        Transaction rule = transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE,
                "Aluguel", expenseCategoryId("Moradia"), new BigDecimal("900.00"), LocalDate.of(2026, 1, 15),
                maria.getId(), conjunta.getId(), null, true, null));

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(put("/api/transactions/" + rule.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body(rule, Map.of("amount", "950.00"))))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                containsString("Edição da regra recorrente não é suportada"));
    }

    @Test
    void movingRecurringOccurrenceRecordsExceptionForOriginalDay() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        Transaction rule = transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE,
                "Aluguel", expenseCategoryId("Moradia"), new BigDecimal("900.00"), LocalDate.of(2026, 1, 15),
                maria.getId(), conjunta.getId(), null, true, null));
        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 20));
        Transaction february = transactionService.getAllTransactions().stream()
                .filter(t -> rule.getId().equals(t.getRecurrenceSourceId()) && t.getDate().equals(LocalDate.of(2026, 2, 15)))
                .findFirst().orElseThrow();

        mockMvc.perform(put("/api/transactions/" + february.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(february, Map.of("date", "2026-02-18", "recurring", false))))
                .andExpect(status().isOk());

        // Reprocessar não recria o dia 15 de fevereiro
        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 20));
        long onOriginalDay = transactionService.getAllTransactions().stream()
                .filter(t -> rule.getId().equals(t.getRecurrenceSourceId()) && t.getDate().equals(LocalDate.of(2026, 2, 15)))
                .count();
        assertEquals(0, onOriginalDay);
    }

    @Test
    void changingTypeIsRefused() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        Transaction expense = accountExpense(maria, conjunta, "100.00", LocalDate.of(2026, 1, 5));

        mockMvc.perform(put("/api/transactions/" + expense.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(expense, Map.of("type", "INCOME", "categoryId", expenseCategoryId("Alimentação")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tipo do lançamento não pode mudar"));
    }

    @Test
    void unknownTransactionIsRejected() throws Exception {
        profile("Maria");
        account("Conjunta", "100");
        mockMvc.perform(put("/api/transactions/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EXPENSE\",\"description\":\"x\",\"categoryId\":" + expenseCategoryId("Alimentação")
                                + ",\"amount\":1,\"date\":\"2026-01-05\",\"profileId\":1,\"accountId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transação não encontrada"));
    }
}
