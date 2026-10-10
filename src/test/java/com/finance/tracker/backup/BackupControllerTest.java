package com.finance.tracker.backup;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRepository;
import com.finance.tracker.transaction.TransactionRequest;
import com.finance.tracker.transaction.TransactionService;
import com.finance.tracker.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BackupControllerTest {

    @Autowired
    private BackupController backupController;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

        @Autowired
        private TransactionRepository transactionRepository;

        @Autowired
        private TransactionService transactionService;

    @Test
    void exportAndImport_shouldPreserveProfilesAndAccounts() throws Exception {
        Profile profile = profileRepository.save(new Profile("Família Backup"));
        BankAccount account = bankAccountRepository.save(new BankAccount(
                "Conta Backup",
                AccountType.CHECKING,
                new BigDecimal("1250.00"),
                profile
        ));
        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, "Despesa Backup", "Casa", new BigDecimal("50.00"),
                java.time.LocalDate.of(2026, 1, 10), profile.getId(), account.getId(), null, false, null
        ));

        ResponseEntity<ByteArrayResource> exported = backupController.exportBackup("json");

        assertEquals(200, exported.getStatusCode().value());
        assertNotNull(exported.getBody());
        String json = new String(exported.getBody().getByteArray(), StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "finance-backup.json",
                "application/json",
                json.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<Map<String, String>> imported = backupController.importBackup(file);

        assertEquals(200, imported.getStatusCode().value());
        assertEquals("Backup importado com sucesso", imported.getBody().get("message"));
        assertEquals(1, profileRepository.findAll().stream()
                .filter(item -> item.getName().equals("Família Backup"))
                .count());
        assertEquals(new BigDecimal("1200.00"), bankAccountRepository.findAll().stream()
                .filter(item -> item.getName().equals("Conta Backup"))
                .findFirst()
                .orElseThrow()
                .getBalance());
        Transaction restored = transactionRepository.findAll().stream()
                .filter(item -> item.getDescription().equals("Despesa Backup"))
                .findFirst()
                .orElseThrow();
        assertEquals("Família Backup", restored.getProfile().getName());
        assertEquals("Conta Backup", restored.getAccount().getName());
    }

    @Test
    void exportAndImportCsv_shouldPreserveProfilesAndAccounts() throws Exception {
        Profile profile = profileRepository.save(new Profile("Família CSV"));
        BankAccount account = bankAccountRepository.save(new BankAccount(
                "Conta CSV",
                AccountType.CHECKING,
                new BigDecimal("850.00"),
                profile
        ));
        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, "Despesa CSV", "Casa", new BigDecimal("25.00"),
                java.time.LocalDate.of(2026, 1, 10), profile.getId(), account.getId(), null, false, null
        ));

        ResponseEntity<ByteArrayResource> exported = backupController.exportBackup("csv");
        String csv = new String(exported.getBody().getByteArray(), StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "finance-backup.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<Map<String, String>> imported = backupController.importBackup(file);

        assertEquals(200, imported.getStatusCode().value());
        assertEquals(new BigDecimal("825.00"), bankAccountRepository.findAll().stream()
                .filter(item -> item.getName().equals("Conta CSV"))
                .findFirst()
                .orElseThrow()
                .getBalance());
        Transaction restored = transactionRepository.findAll().stream()
                .filter(item -> item.getDescription().equals("Despesa CSV"))
                .findFirst()
                .orElseThrow();
        assertEquals("Família CSV", restored.getProfile().getName());
        assertEquals("Conta CSV", restored.getAccount().getName());
    }

    @Test
    void importInvalidFile_shouldRejectBackup() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.json",
                "application/json",
                "{ invalid".getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> backupController.importBackup(file));

        assertEquals("Arquivo de backup inválido ou corrompido", exception.getMessage());
    }

    @Test
    void import_shouldReplaceRecordsThatAreMissingFromBackup() throws Exception {
        Profile importedProfile = profileRepository.save(new Profile("Perfil Importado"));
        bankAccountRepository.save(new BankAccount("Conta Importada", AccountType.CHECKING,
                new BigDecimal("500.00"), importedProfile));
        ResponseEntity<ByteArrayResource> exported = backupController.exportBackup("json");

        Profile staleProfile = profileRepository.save(new Profile("Perfil Antigo"));
        bankAccountRepository.save(new BankAccount("Conta Antiga", AccountType.CHECKING,
                new BigDecimal("10.00"), staleProfile));

        MockMultipartFile file = new MockMultipartFile(
                "file", "finance-backup.json", "application/json",
                exported.getBody().getByteArray()
        );
        backupController.importBackup(file);

        assertEquals(0, profileRepository.findAll().stream()
                .filter(item -> item.getName().equals("Perfil Antigo"))
                .count());
        assertEquals(1, profileRepository.findAll().stream()
                .filter(item -> item.getName().equals("Perfil Importado"))
                .count());
    }
}