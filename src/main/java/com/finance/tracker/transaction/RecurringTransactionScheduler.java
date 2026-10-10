package com.finance.tracker.transaction;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RecurringTransactionScheduler {

    private final TransactionService transactionService;

    public RecurringTransactionScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Também na inicialização, porque o PC pode estar desligado às 00:05 (D25)
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *")
    public void processDueTransactions() {
        transactionService.processRecurringTransactions(LocalDate.now());
    }
}
