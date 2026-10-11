package com.finance.tracker.transaction;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuickEntryApiTest extends IntegrationTest {

    @Test
    void cardInstallmentsCreateThreeMonths() throws Exception {
        Profile maria = profile("Maria");
        CreditCard nubank = card("Nubank", "3000");
        Map<String, Object> body = new HashMap<>();
        body.put("type", "EXPENSE");
        body.put("description", "Mercado");
        body.put("categoryId", category("Alimentação", CategoryType.EXPENSE).getId());
        body.put("amount", "300.00");
        body.put("date", "2026-01-10");
        body.put("profileId", maria.getId());
        body.put("creditCardId", nubank.getId());
        body.put("installmentCount", 3);

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions").param("month", "2026-01"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Mercado - Parcela 1/3"));
        mockMvc.perform(get("/api/transactions").param("month", "2026-02"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Mercado - Parcela 2/3"));
        mockMvc.perform(get("/api/transactions").param("month", "2026-03"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Mercado - Parcela 3/3"));
    }
}
