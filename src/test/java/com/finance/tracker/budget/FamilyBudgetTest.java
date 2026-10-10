package com.finance.tracker.budget;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.profile.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FamilyBudgetTest extends IntegrationTest {

    @Test
    void summaryAddsEveryProfileInTheMonth() throws Exception {
        Long food = expenseCategoryId("Alimentação");
        createBudget(food, "500.00", "2026-01");
        Profile maria = profile("Maria");
        Profile joao = profile("João");
        BankAccount account = account("Conjunta", "1000.00");
        CreditCard card = card("Nubank", "2000.00");

        accountExpense(maria, account, "200.00", LocalDate.of(2026, 1, 10));
        cardExpense(joao, card, "100.00", LocalDate.of(2026, 1, 12), null);
        accountExpense(maria, account, "50.00", LocalDate.of(2026, 2, 5));

        mockMvc.perform(get("/api/budgets/summary").param("month", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(food.intValue()))
                .andExpect(jsonPath("$[0].category").value("Alimentação"))
                .andExpect(content().string(containsString("\"consumedAmount\":300.00")))
                .andExpect(content().string(containsString("\"consumedPercentage\":60.00")));
    }

    @Test
    void cardInstallmentsCountInTheirOwnMonth() throws Exception {
        Long food = expenseCategoryId("Alimentação");
        for (String month : new String[]{"2026-01", "2026-02", "2026-03"}) {
            createBudget(food, "1000.00", month);
        }
        cardExpense(profile("Maria"), card("Nubank", "2000.00"), "300.00", LocalDate.of(2026, 1, 10), 3);

        for (String month : new String[]{"2026-01", "2026-02", "2026-03"}) {
            mockMvc.perform(get("/api/budgets/summary").param("month", month))
                    .andExpect(content().string(containsString("\"consumedAmount\":100.00")));
        }
    }

    @Test
    void duplicateBudgetIsRejected() throws Exception {
        Long food = expenseCategoryId("Alimentação");
        createBudget(food, "500.00", "2026-01");

        mockMvc.perform(post("/api/budgets").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", food, "limitAmount", "300.00", "month", "2026-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já existe orçamento para esta categoria neste mês"));
    }

    private void createBudget(Long categoryId, String limit, String month) throws Exception {
        mockMvc.perform(post("/api/budgets").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", categoryId, "limitAmount", limit, "month", month))))
                .andExpect(status().isCreated());
    }
}
