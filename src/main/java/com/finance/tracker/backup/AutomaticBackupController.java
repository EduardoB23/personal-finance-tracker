package com.finance.tracker.backup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/backup")
public class AutomaticBackupController {

    public record StatusView(LocalDateTime lastSuccessAt, String lastFile, String lastError, LocalDateTime lastErrorAt) {
    }

    private final AutomaticBackupService service;

    public AutomaticBackupController(AutomaticBackupService service) {
        this.service = service;
    }

    @GetMapping("/automatic-status")
    public StatusView status() {
        AutomaticBackupService.Status status = service.status();
        return new StatusView(status.lastSuccessAt(), status.lastFile(), status.lastError(), status.lastErrorAt());
    }
}
