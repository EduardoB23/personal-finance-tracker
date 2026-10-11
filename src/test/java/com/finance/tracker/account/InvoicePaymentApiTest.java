package com.finance.tracker.account;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Fatura de 2026-02: compra em 10/01 cai no mês seguinte (fechamento dia 20)
class InvoicePaymentApiTest extends IntegrationTest {

    @Test
    void partialPaymentLeavesRemaining() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);

        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5))
                .getResponse();
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paidAmount").value(300.0))
                .andExpect(jsonPath("$[0].remainingAmount").value(700.0))
                .andExpect(jsonPath("$[0].status").value("Parcial"));
    }

    @Test
    void paymentOfRemainingMarksPaid() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));

        payInvoice(nubank, "2026-02", conjunta, maria, "700.00", LocalDate.of(2026, 2, 6));
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].remainingAmount").value(0.0))
                .andExpect(jsonPath("$[0].status").value("Paga"));
    }

    @Test
    void invoiceWithoutPaymentsIsAberta() throws Exception {
        Profile maria = profile("Maria");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);

        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].status").value("Aberta"))
                .andExpect(jsonPath("$[0].paidAmount").value(0.0));
    }

    @Test
    void paymentMovesAccountBalanceAndLimit() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        assertEquals(0, new java.math.BigDecimal("2000").compareTo(availableLimitOf(nubank)));

        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));

        assertEquals(0, new java.math.BigDecimal("4700").compareTo(balanceOf(conjunta)));
        assertEquals(0, new java.math.BigDecimal("2300").compareTo(availableLimitOf(nubank)));
    }

    @Test
    void paymentIsNotATransaction() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        int before = transactionService.getAllTransactions().size();

        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));

        assertEquals(before, transactionService.getAllTransactions().size());
    }

    @Test
    void zeroOrNegativeAmountIsRejected() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);

        for (String amount : new String[]{"0", "-5"}) {
            org.springframework.test.web.servlet.MvcResult result = payInvoice(nubank, "2026-02", conjunta, maria, amount, LocalDate.of(2026, 2, 5));
            assertEquals(400, result.getResponse().getStatus());
            org.hamcrest.MatcherAssert.assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                    org.hamcrest.Matchers.containsString("Valor deve ser maior que zero"));
        }
    }

    @Test
    void overpaymentIsRejectedWithoutChanges() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);

        org.springframework.test.web.servlet.MvcResult result = payInvoice(nubank, "2026-02", conjunta, maria, "1001.00", LocalDate.of(2026, 2, 5));

        assertEquals(400, result.getResponse().getStatus());
        org.hamcrest.MatcherAssert.assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                org.hamcrest.Matchers.containsString("Valor maior que o saldo da fatura"));
        assertEquals(0, new java.math.BigDecimal("5000").compareTo(balanceOf(conjunta)));
        assertEquals(0, new java.math.BigDecimal("2000").compareTo(availableLimitOf(nubank)));
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].remainingAmount").value(1000.0))
                .andExpect(jsonPath("$[0].payments", org.hamcrest.Matchers.empty()));
    }

    @Test
    void missingInvoiceIsRejected() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");

        org.springframework.test.web.servlet.MvcResult result = payInvoice(nubank, "2026-02", conjunta, maria, "10.00", LocalDate.of(2026, 2, 5));

        assertEquals(400, result.getResponse().getStatus());
        org.hamcrest.MatcherAssert.assertThat(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                org.hamcrest.Matchers.containsString("Fatura não encontrada"));
    }

    @Test
    void unknownAccountOrProfileIsRejected() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        BankAccount ghost = new BankAccount("Fantasma", AccountType.CHECKING, java.math.BigDecimal.ONE);
        ghost.setId(999999L);

        org.springframework.test.web.servlet.MvcResult noAccount = payInvoice(nubank, "2026-02", ghost, maria, "10.00", LocalDate.of(2026, 2, 5));
        org.springframework.test.web.servlet.MvcResult noProfile = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/credit-cards/" + nubank.getId() + "/invoices/2026-02/payments")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + conjunta.getId() + ",\"profileId\":999999,\"amount\":10,\"date\":\"2026-02-05\"}"))
                .andReturn();

        org.hamcrest.MatcherAssert.assertThat(noAccount.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                org.hamcrest.Matchers.containsString("Conta não encontrada"));
        org.hamcrest.MatcherAssert.assertThat(noProfile.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                org.hamcrest.Matchers.containsString("Perfil não encontrado"));
    }

    @Test
    void purchaseAfterPartialPaymentRaisesRemaining() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));

        cardExpense(maria, nubank, "100.00", LocalDate.of(2026, 1, 12), 1);

        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].paidAmount").value(300.0))
                .andExpect(jsonPath("$[0].remainingAmount").value(800.0))
                .andExpect(jsonPath("$[0].status").value("Parcial"));
    }
}
