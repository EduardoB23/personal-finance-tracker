package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvoiceDeletionTest extends IntegrationTest {

    // Fatura de 2026-02 com duas compras: 850 e 150 (total 1000)
    private Transaction[] twoPurchases(Profile maria, CreditCard nubank) {
        Transaction big = cardExpense(maria, nubank, "850.00", LocalDate.of(2026, 1, 10), 1);
        Transaction small = cardExpense(maria, nubank, "150.00", LocalDate.of(2026, 1, 11), 1);
        return new Transaction[]{big, small};
    }

    @Test
    void purchaseDeletionRefusedWhenPaymentsWouldGoNegative() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        Transaction[] purchases = twoPurchases(maria, nubank);
        payInvoice(nubank, "2026-02", conjunta, maria, "900.00", LocalDate.of(2026, 2, 5));

        mockMvc.perform(delete("/api/transactions/" + purchases[1].getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fatura já tem pagamento que impede esta exclusão"));
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].remainingAmount").value(100.0));
    }

    @Test
    void refundRestoresBalanceLimitAndRemaining() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        twoPurchases(maria, nubank);
        MvcResult paid = payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));
        long paymentId = Long.parseLong(com.jayway.jsonpath.JsonPath.read(paid.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.id").toString());

        mockMvc.perform(delete("/api/invoice-payments/" + paymentId)).andExpect(status().isNoContent());

        assertEquals(0, new BigDecimal("5000").compareTo(balanceOf(conjunta)));
        assertEquals(0, new BigDecimal("2000").compareTo(availableLimitOf(nubank)));
        mockMvc.perform(get("/api/credit-cards/" + nubank.getId() + "/invoices"))
                .andExpect(jsonPath("$[0].remainingAmount").value(1000.0))
                .andExpect(jsonPath("$[0].status").value("Aberta"));
    }

    @Test
    void refundUnknownIdIsRejected() throws Exception {
        mockMvc.perform(delete("/api/invoice-payments/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pagamento não encontrado"));
    }

    @Test
    void purchaseDeletableAfterRefund() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        Transaction[] purchases = twoPurchases(maria, nubank);
        MvcResult paid = payInvoice(nubank, "2026-02", conjunta, maria, "900.00", LocalDate.of(2026, 2, 5));
        long paymentId = Long.parseLong(com.jayway.jsonpath.JsonPath.read(paid.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.id").toString());
        mockMvc.perform(delete("/api/invoice-payments/" + paymentId)).andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/transactions/" + purchases[1].getId())).andExpect(status().isNoContent());

        assertEquals(0, new BigDecimal("2150").compareTo(availableLimitOf(nubank)));
    }
}
