package com.finance.tracker.account;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FamilyAccountsTest extends IntegrationTest {

    @Test
    void accountsAndCardsHaveNoOwner() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Conjunta", "type", "CHECKING", "balance", "1000.00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/credit-cards").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nubank", "limitAmount", "2000.00", "availableLimit", "2000.00",
                                "closingDay", 20, "dueDay", 10))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts"))
                .andExpect(jsonPath("$[*]", everyItem(not(hasKey("profile")))))
                .andExpect(jsonPath("$[*]", everyItem(not(hasKey("profileId")))));
        mockMvc.perform(get("/api/credit-cards"))
                .andExpect(jsonPath("$[*]", everyItem(not(hasKey("profile")))))
                .andExpect(jsonPath("$[*]", everyItem(not(hasKey("profileId")))));
    }

    @Test
    void anyProfileUsesAnyAccountOrCard() throws Exception {
        Profile maria = profile("Maria");
        Profile joao = profile("João");
        BankAccount joint = account("Conjunta", "1000.00");
        CreditCard nubank = card("Nubank", "2000.00");
        Long food = expenseCategoryId("Alimentação");

        postExpense(Map.of("type", "EXPENSE", "description", "Mercado", "categoryId", food, "amount", "100.00",
                "date", "2026-01-10", "profileId", maria.getId(), "accountId", joint.getId()), maria);
        postExpense(Map.of("type", "EXPENSE", "description", "Farmácia", "categoryId", food, "amount", "40.00",
                "date", "2026-01-10", "profileId", maria.getId(), "creditCardId", nubank.getId()), maria);
        postExpense(Map.of("type", "EXPENSE", "description", "Padaria", "categoryId", food, "amount", "20.00",
                "date", "2026-01-11", "profileId", joao.getId(), "accountId", joint.getId()), joao);
    }

    private void postExpense(Map<String, Object> body, Profile author) throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileId").value(author.getId().intValue()));
    }
}
