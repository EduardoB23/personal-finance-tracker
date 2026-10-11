package com.finance.tracker.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.tracker.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AutomaticBackupServiceTest extends IntegrationTest {

    @Autowired private BackupController backupController;
    @Autowired private ObjectMapper jsonMapper;
    @Value("${finance.backup-dir}") private String configuredFolder;

    @TempDir Path tmp;

    private AutomaticBackupService serviceOn(Path folder, LocalDate day) {
        Clock clock = Clock.fixed(day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new AutomaticBackupService(backupController, folder.toString(), clock);
    }

    @Test
    void startupWritesTodaysBackup() {
        LocalDate today = LocalDate.of(2026, 10, 11);
        serviceOn(tmp, today).onStartup();
        assertTrue(Files.exists(tmp.resolve("finance-backup-2026-10-11.json")));
    }

    @Test
    void startupSkipsWhenTodaysBackupExists() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 11);
        Path existing = tmp.resolve("finance-backup-2026-10-11.json");
        Files.writeString(existing, "marcador", StandardCharsets.UTF_8);

        serviceOn(tmp, today).onStartup();

        assertEquals("marcador", Files.readString(existing, StandardCharsets.UTF_8));
    }

    @Test
    void dailyJobWritesFileAtZeroTenAndIsScheduled() throws Exception {
        LocalDate day = LocalDate.of(2026, 10, 12);
        serviceOn(tmp, day).runDaily();
        assertTrue(Files.exists(tmp.resolve("finance-backup-2026-10-12.json")));
        assertEquals("0 10 0 * * *", AutomaticBackupService.class.getMethod("runDaily")
                .getAnnotation(Scheduled.class).cron());
    }

    @Test
    void keepsLastThirtyFiles() throws Exception {
        for (int day = 1; day <= 31; day++) {
            Files.writeString(tmp.resolve(String.format("finance-backup-2026-10-%02d.json", day)), "{}");
        }

        serviceOn(tmp, LocalDate.of(2026, 11, 1)).runBackup();

        try (Stream<Path> files = Files.list(tmp)) {
            assertEquals(30, files.count());
        }
        // 30 mais recentes: o de hoje (11-01) mais 10-31 a 10-03; saem 10-02 e 10-01
        assertFalse(Files.exists(tmp.resolve("finance-backup-2026-10-01.json")));
        assertFalse(Files.exists(tmp.resolve("finance-backup-2026-10-02.json")));
        assertTrue(Files.exists(tmp.resolve("finance-backup-2026-10-03.json")));
        assertTrue(Files.exists(tmp.resolve("finance-backup-2026-11-01.json")));
    }

    @Test
    void generatedFileRoundTrips() throws Exception {
        serviceOn(tmp, LocalDate.of(2026, 10, 13)).runBackup();

        String content = Files.readString(tmp.resolve("finance-backup-2026-10-13.json"), StandardCharsets.UTF_8);
        BackupExport export = jsonMapper.readValue(content, BackupExport.class);
        assertNotNull(export.invoicePayments());
    }

    @Test
    void missingFolderRecordsErrorAndDoesNotCrash() throws Exception {
        Path blocker = tmp.resolve("arquivo");
        Files.writeString(blocker, "não é pasta");
        AutomaticBackupService service = serviceOn(blocker, LocalDate.of(2026, 10, 14));

        service.runBackup();

        assertNotNull(service.status().lastError());
        assertNotNull(service.status().lastErrorAt());
    }

    @Test
    void successClearsPreviousError() throws Exception {
        Path blocker = tmp.resolve("pasta");
        Files.writeString(blocker, "é arquivo, não pasta");
        AutomaticBackupService service = serviceOn(blocker, LocalDate.of(2026, 10, 15));
        service.runBackup();
        assertNotNull(service.status().lastError());

        Files.delete(blocker);
        service.runBackup();

        assertNull(service.status().lastError());
        assertEquals("finance-backup-2026-10-15.json", service.status().lastFile());
    }

    @Test
    void defaultFolderIsOneDrive() {
        // Os testes apontam para target/, para nunca gravar no OneDrive do usuário. O padrão de produção
        // (${user.home}/OneDrive quando FINANCE_BACKUP_DIR não existe) fica sem prova automática; ver .checks/backup-automatico.md
        assertEquals("target/test-backups", configuredFolder);
    }
}
