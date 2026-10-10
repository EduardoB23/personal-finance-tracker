package com.finance.tracker.backup;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.budget.BudgetRepository;
import com.finance.tracker.budget.BudgetRequest;
import com.finance.tracker.budget.BudgetService;
import com.finance.tracker.category.Category;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRequest;
import com.finance.tracker.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackupControllerTest extends IntegrationTest {

    @Autowired private BackupController backupController;
    @Autowired private BudgetService budgetService;
    @Autowired private BudgetRepository budgetRepository;

    @Test
    void jsonRoundTripPreservesStructure() throws Exception {
        roundTrip("json", "finance-backup.json", "application/json");
    }

    @Test
    void csvRoundTripPreservesStructure() throws Exception {
        roundTrip("csv", "finance-backup.csv", "text/csv");
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        Profile profile = profile("Maria");
        BankAccount account = account("Conta", "100.00");
        accountExpense(profile, account, "10.00", LocalDate.of(2026, 1, 10));
        String json = exported("json").replaceAll("\"categoryId\"\\s*:\\s*\\d+", "\"categoryId\" : 999999");

        mockMvc.perform(multipart("/api/backup/import")
                        .file(new MockMultipartFile("file", "finance-backup.json", "application/json",
                                json.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Backup inválido")));
    }

    @Test
    void importInvalidFile_shouldRejectBackup() {
        MockMultipartFile file = new MockMultipartFile("file", "invalid.json", "application/json",
                "{ invalid".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> backupController.importBackup(file));

        assertEquals("Arquivo de backup inválido ou corrompido", exception.getMessage());
    }

    @Test
    void import_shouldReplaceRecordsThatAreMissingFromBackup() throws Exception {
        profile("Perfil Importado");
        String backup = exported("json");
        profile("Perfil Antigo");

        backupController.importBackup(new MockMultipartFile("file", "finance-backup.json", "application/json",
                backup.getBytes(StandardCharsets.UTF_8)));

        List<String> names = profileRepository.findAll().stream().map(Profile::getName).toList();
        assertTrue(names.contains("Perfil Importado"));
        assertFalse(names.contains("Perfil Antigo"));
    }

    private void roundTrip(String format, String filename, String contentType) throws Exception {
        Profile profile = profile("Maria");
        BankAccount account = account("Corrente", "5000.00");
        CreditCard card = card("Nubank", "2000.00");
        Category pets = categoryRepository.save(new Category("Pets", CategoryType.EXPENSE));
        Category archived = categoryRepository.save(new Category("Antiga", CategoryType.EXPENSE));
        archived.setArchived(true);

        transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE, "Ração", pets.getId(),
                new BigDecimal("80.00"), LocalDate.of(2026, 1, 8), profile.getId(), account.getId(), null, false, null));
        cardExpense(profile, card, "300.00", LocalDate.of(2026, 1, 10), 3);
        Transaction rule = transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE,
                "Aluguel", expenseCategoryId("Moradia"), new BigDecimal("900.00"), LocalDate.of(2026, 1, 15),
                profile.getId(), account.getId(), null, true, null));
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));
        Transaction february = transactionService.getAllTransactions().stream()
                .filter(t -> rule.getId().equals(t.getRecurrenceSourceId()) && t.getDate().equals(LocalDate.of(2026, 2, 15)))
                .findFirst().orElseThrow();
        transactionService.deleteTransaction(february.getId());
        budgetService.createBudget(new BudgetRequest(expenseCategoryId("Alimentação"), new BigDecimal("500.00"), YearMonth.of(2026, 1)));
        entityManager.flush();
        long categoriesBefore = categoryRepository.count();
        int transactionsBefore = transactionService.getAllTransactions().size();

        String backup = exported(format);
        ResponseEntity<Map<String, String>> imported = backupController.importBackup(
                new MockMultipartFile("file", filename, contentType, backup.getBytes(StandardCharsets.UTF_8)));
        assertEquals(200, imported.getStatusCode().value());

        // Categorias voltam com nome, tipo e arquivamento
        assertEquals(categoriesBefore, categoryRepository.count());
        assertFalse(category("Pets", CategoryType.EXPENSE).isArchived());
        assertTrue(category("Antiga", CategoryType.EXPENSE).isArchived());

        // Lançamentos e orçamentos apontam para as categorias restauradas
        Transaction food = transactionService.getAllTransactions().stream()
                .filter(t -> t.getDescription().equals("Ração")).findFirst().orElseThrow();
        assertEquals(category("Pets", CategoryType.EXPENSE).getId(), food.getCategoryId());
        assertEquals(category("Alimentação", CategoryType.EXPENSE).getId(), budgetRepository.findAll().get(0).getCategoryId());

        // Processar a recorrência não duplica ocorrências nem recria a excluída
        transactionService.processRecurringTransactions(LocalDate.of(2026, 3, 15));
        List<Transaction> afterProcessing = transactionService.getAllTransactions();
        assertEquals(transactionsBefore, afterProcessing.size());
        assertTrue(afterProcessing.stream().noneMatch(t -> t.getDate().equals(LocalDate.of(2026, 2, 15))));

        // Excluir uma parcela remove as 3
        Transaction installment = afterProcessing.stream()
                .filter(t -> t.getDescription().contains("Parcela 2/3")).findFirst().orElseThrow();
        transactionService.deleteTransaction(installment.getId());
        assertTrue(transactionService.getAllTransactions().stream().noneMatch(t -> t.getDescription().contains("Parcela")));
    }

    private String exported(String format) throws Exception {
        entityManager.flush();
        ResponseEntity<ByteArrayResource> response = backupController.exportBackup(format);
        assertEquals(200, response.getStatusCode().value());
        return new String(response.getBody().getByteArray(), StandardCharsets.UTF_8);
    }
}
