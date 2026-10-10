package com.finance.tracker.backup;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// Formato do backup (JSON e CSV). Os ids são os do banco de origem e são remapeados na importação.
public record BackupExport(
        List<ProfileData> profiles,
        List<CategoryData> categories,
        List<AccountData> accounts,
        List<CardData> cards,
        List<InvoiceData> invoices,
        List<BudgetData> budgets,
        List<GoalData> goals,
        List<TransactionData> transactions,
        List<RecurrenceExceptionData> recurrenceExceptions
) {
    public record ProfileData(Long id, String name) {}

    public record CategoryData(Long id, String name, CategoryType type, boolean archived) {}

    public record AccountData(Long id, String name, AccountType type, BigDecimal balance) {}

    public record CardData(Long id, String name, BigDecimal limitAmount, BigDecimal availableLimit,
                           Integer closingDay, Integer dueDay) {}

    public record InvoiceData(Long id, Long creditCardId, YearMonth billingMonth, BigDecimal amount, LocalDate dueDate) {}

    public record BudgetData(Long id, Long categoryId, BigDecimal limitAmount, YearMonth month) {}

    public record GoalData(Long id, String title, BigDecimal targetAmount, BigDecimal currentAmount, Long profileId) {}

    public record TransactionData(Long id, TransactionType type, String description, Long categoryId,
                                  BigDecimal amount, LocalDate date, Long profileId, Long accountId,
                                  Long targetAccountId, Long creditCardId, boolean recurring, Integer installmentCount,
                                  Long installmentGroupId, Long recurrenceSourceId) {}

    public record RecurrenceExceptionData(Long sourceTransactionId, LocalDate occurrenceDate) {}
}
