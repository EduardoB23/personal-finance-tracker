package com.finance.tracker.backup;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record BackupExport(
        List<ProfileData> profiles,
        List<AccountData> accounts,
        List<CardData> cards,
        List<InvoiceData> invoices,
        List<BudgetData> budgets,
        List<GoalData> goals,
        List<TransactionData> transactions
) {
    public record ProfileData(Long id, String name) {}

    public record AccountData(Long id, String name, AccountType type, BigDecimal balance, Long profileId) {}

    public record CardData(Long id, String name, BigDecimal limitAmount, BigDecimal availableLimit,
                           Integer closingDay, Integer dueDay, Long profileId) {}

        public record InvoiceData(Long id, Long creditCardId, YearMonth billingMonth, BigDecimal amount, LocalDate dueDate) {}

    public record BudgetData(Long id, String category, BigDecimal limitAmount, YearMonth month, Long profileId) {}

    public record GoalData(Long id, String title, BigDecimal targetAmount, BigDecimal currentAmount, Long profileId) {}

    public record TransactionData(Long id, TransactionType type, String description, String category,
                                  BigDecimal amount, LocalDate date, Long profileId, Long accountId,
                                  Long targetAccountId, Long creditCardId, boolean recurring, Integer installmentCount) {}
}
