package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionQueryApiTest extends IntegrationTest {

    @Test
    void monthFilterReturnsOnlyThatMonthNewestFirst() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "1000");
        accountExpense(maria, conjunta, "10.00", LocalDate.of(2026, 1, 5));
        accountExpense(maria, conjunta, "20.00", LocalDate.of(2026, 1, 31));
        accountExpense(maria, conjunta, "30.00", LocalDate.of(2026, 2, 1));

        mockMvc.perform(get("/api/transactions").param("month", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-01-31"))
                .andExpect(jsonPath("$[1].date").value("2026-01-05"));
    }

    @Test
    void profileAndMonthFilter() throws Exception {
        Profile maria = profile("Maria");
        Profile joao = profile("João");
        BankAccount conjunta = account("Conjunta", "1000");
        accountExpense(maria, conjunta, "10.00", LocalDate.of(2026, 1, 5));
        accountExpense(joao, conjunta, "20.00", LocalDate.of(2026, 1, 6));

        mockMvc.perform(get("/api/transactions")
                        .param("month", "2026-01")
                        .param("profileId", String.valueOf(maria.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].profileId").value(maria.getId()));
    }

    @Test
    void invalidMonthReturns400() throws Exception {
        mockMvc.perform(get("/api/transactions").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mês inválido, use o formato AAAA-MM"));

        mockMvc.perform(get("/api/transactions").param("month", "01/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withoutParamsReturnsEverything() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "1000");
        accountExpense(maria, conjunta, "10.00", LocalDate.of(2026, 1, 5));
        accountExpense(maria, conjunta, "30.00", LocalDate.of(2026, 2, 1));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].date", hasItem("2026-01-05")))
                .andExpect(jsonPath("$[*].date", hasItem("2026-02-01")));
    }
}
