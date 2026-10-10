package com.finance.tracker.category;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiTest extends IntegrationTest {

    @Test
    void defaultCategoriesAreSeeded() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[?(@.type == 'EXPENSE')].name", containsInAnyOrder(
                        "Alimentação", "Moradia", "Transporte", "Lazer", "Saúde", "Educação", "Outros")))
                .andExpect(jsonPath("$[?(@.type == 'INCOME')].name", containsInAnyOrder("Salário", "Outros")))
                .andExpect(jsonPath("$[*].archived", everyItem(is(false))));
    }

    @Test
    void createdCategoryIsListedByType() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Pets", "type", "EXPENSE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Pets"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/api/categories").param("type", "EXPENSE"))
                .andExpect(jsonPath("$[*].name", hasItem("Pets")));
        mockMvc.perform(get("/api/categories").param("type", "INCOME"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Pets"))));
    }

    @Test
    void duplicateCategoryIsRejected() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "alimentação", "type", "EXPENSE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Categoria já existe"));
    }

    @Test
    void deletedCategoryIsArchived() throws Exception {
        Category pets = categoryRepository.save(new Category("Pets", CategoryType.EXPENSE));
        Profile profile = profile("Maria");
        BankAccount account = account("Conta", "100.00");
        Transaction expense = transactionService.createTransaction(new com.finance.tracker.transaction.TransactionRequest(
                com.finance.tracker.transaction.TransactionType.EXPENSE, "Ração", pets.getId(),
                new java.math.BigDecimal("30.00"), LocalDate.of(2026, 1, 5), profile.getId(), account.getId(),
                null, false, null));

        mockMvc.perform(delete("/api/categories/{id}", pets.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Pets"))));
        mockMvc.perform(get("/api/categories").param("includeArchived", "true"))
                .andExpect(jsonPath("$[?(@.name == 'Pets')].archived", contains(true)));
        mockMvc.perform(get("/api/transactions"))
                .andExpect(jsonPath("$[?(@.id == " + expense.getId() + ")].categoryId", contains(pets.getId().intValue())));
    }

    @Test
    void invalidCategoryIsRejected() throws Exception {
        Profile profile = profile("Maria");
        BankAccount account = account("Conta", "100.00");
        Category archived = categoryRepository.save(new Category("Antiga", CategoryType.EXPENSE));
        archived.setArchived(true);
        Long salary = category("Salário", CategoryType.INCOME).getId();

        for (Long categoryId : new Long[]{999_999L, archived.getId(), salary}) {
            mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                            .content(json(expenseBody(profile, account, categoryId))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Categoria inválida para este lançamento"));
        }
    }

    @Test
    void categoryIsRequiredExceptForTransfers() throws Exception {
        Profile profile = profile("Maria");
        BankAccount checking = account("Corrente", "100.00");
        BankAccount savings = account("Poupança", "0.00");

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "TRANSFER", "description", "Guardar", "amount", "50.00",
                                "date", "2026-01-10", "profileId", profile.getId(), "accountId", checking.getId(),
                                "targetAccountId", savings.getId()))))
                .andExpect(status().isCreated());

        for (String type : new String[]{"EXPENSE", "INCOME"}) {
            mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("type", type, "description", "Sem categoria", "amount", "10.00",
                                    "date", "2026-01-10", "profileId", profile.getId(), "accountId", checking.getId()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.categoryId").exists());
        }
    }

    private Map<String, Object> expenseBody(Profile profile, BankAccount account, Long categoryId) {
        return Map.of("type", "EXPENSE", "description", "Compra", "categoryId", categoryId, "amount", "10.00",
                "date", "2026-01-10", "profileId", profile.getId(), "accountId", account.getId());
    }
}
