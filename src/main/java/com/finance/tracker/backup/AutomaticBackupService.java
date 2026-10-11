package com.finance.tracker.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// Backup diário em JSON numa pasta configurável (D24). Roda também na inicialização (D25).
@Service
public class AutomaticBackupService {

    static final int KEEP = 30;
    static final String PREFIX = "finance-backup-";
    static final String SUFFIX = ".json";

    private static final Logger log = LoggerFactory.getLogger(AutomaticBackupService.class);

    public record Status(LocalDateTime lastSuccessAt, String lastFile, String lastError, LocalDateTime lastErrorAt) {
    }

    private final BackupController backupController;
    private final Path folder;
    private final Clock clock;
    private Status status = new Status(null, null, null, null);

    public AutomaticBackupService(BackupController backupController,
                                  @Value("${finance.backup-dir}") String folder,
                                  Clock clock) {
        this.backupController = backupController;
        this.folder = Path.of(folder);
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!Files.exists(folder.resolve(fileNameFor(LocalDate.now(clock))))) {
            runBackup();
        }
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void runDaily() {
        runBackup();
    }

    // Grava o backup de hoje e mantém só os últimos 30; falha vira aviso, não derruba o servidor
    public synchronized void runBackup() {
        LocalDate today = LocalDate.now(clock);
        try {
            Files.createDirectories(folder);
            byte[] content = backupController.exportBackup("json").getBody().getByteArray();
            Path target = folder.resolve(fileNameFor(today));
            Files.write(target, content);
            prune();
            status = new Status(LocalDateTime.now(clock), target.getFileName().toString(), null, null);
        } catch (Exception e) {
            log.warn("Backup automático falhou em {}: {}", folder, e.getMessage());
            status = new Status(status.lastSuccessAt(), status.lastFile(), e.getMessage(), LocalDateTime.now(clock));
        }
    }

    public Status status() {
        return status;
    }

    private void prune() throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            List<Path> backups = files
                    .filter(path -> path.getFileName().toString().startsWith(PREFIX))
                    .filter(path -> path.getFileName().toString().endsWith(SUFFIX))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .toList();
            for (Path old : backups.subList(Math.min(KEEP, backups.size()), backups.size())) {
                Files.delete(old);
            }
        }
    }

    static String fileNameFor(LocalDate date) {
        return PREFIX + date + SUFFIX;
    }
}
