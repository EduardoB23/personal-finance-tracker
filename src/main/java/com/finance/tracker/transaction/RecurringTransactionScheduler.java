package com.finance.tracker.transaction;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RecurringTransactionScheduler {

    private final TransactionService transactionService;

    public RecurringTransactionScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void processDueTransactions() {
        transactionService.processRecurringTransactions(LocalDate.now());
    }
}