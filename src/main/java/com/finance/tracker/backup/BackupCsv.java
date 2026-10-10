package com.finance.tracker.backup;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// CSV do backup: uma linha por registro, coluna "entity" com o tipo e as demais lidas pelo nome do cabeçalho
final class BackupCsv {

    static final List<String> COLUMNS = List.of(
            "entity", "id", "name", "type", "archived", "balance", "limitAmount", "availableLimit", "closingDay",
            "dueDay", "creditCardId", "billingMonth", "amount", "dueDate", "categoryId", "month", "title",
            "targetAmount", "currentAmount", "profileId", "description", "date", "accountId", "targetAccountId",
            "recurring", "installmentCount", "installmentGroupId", "recurrenceSourceId", "sourceTransactionId",
            "occurrenceDate");

    private BackupCsv() {
    }

    static String write(BackupExport export) {
        StringBuilder csv = new StringBuilder(String.join(",", COLUMNS)).append('\n');
        export.profiles().forEach(p -> row(csv, "PROFILE", Map.of("id", p.id(), "name", p.name())));
        export.categories().forEach(c -> row(csv, "CATEGORY",
                Map.of("id", c.id(), "name", c.name(), "type", c.type(), "archived", c.archived())));
        export.accounts().forEach(a -> row(csv, "ACCOUNT",
                Map.of("id", a.id(), "name", a.name(), "type", a.type(), "balance", a.balance())));
        export.cards().forEach(c -> row(csv, "CARD", Map.of("id", c.id(), "name", c.name(),
                "limitAmount", c.limitAmount(), "availableLimit", c.availableLimit(),
                "closingDay", c.closingDay(), "dueDay", c.dueDay())));
        export.invoices().forEach(i -> row(csv, "INVOICE", Map.of("id", i.id(), "creditCardId", i.creditCardId(),
                "billingMonth", i.billingMonth(), "amount", i.amount(), "dueDate", i.dueDate())));
        export.budgets().forEach(b -> row(csv, "BUDGET", Map.of("id", b.id(), "categoryId", b.categoryId(),
                "limitAmount", b.limitAmount(), "month", b.month())));
        export.goals().forEach(g -> row(csv, "GOAL", Map.of("id", g.id(), "title", g.title(),
                "targetAmount", g.targetAmount(), "currentAmount", g.currentAmount(), "profileId", g.profileId())));
        export.transactions().forEach(t -> {
            Map<String, Object> values = new HashMap<>();
            values.put("id", t.id());
            values.put("type", t.type());
            values.put("description", t.description());
            values.put("categoryId", t.categoryId());
            values.put("amount", t.amount());
            values.put("date", t.date());
            values.put("profileId", t.profileId());
            values.put("accountId", t.accountId());
            values.put("targetAccountId", t.targetAccountId());
            values.put("creditCardId", t.creditCardId());
            values.put("recurring", t.recurring());
            values.put("installmentCount", t.installmentCount());
            values.put("installmentGroupId", t.installmentGroupId());
            values.put("recurrenceSourceId", t.recurrenceSourceId());
            row(csv, "TRANSACTION", values);
        });
        export.recurrenceExceptions().forEach(e -> row(csv, "RECURRENCE_EXCEPTION",
                Map.of("sourceTransactionId", e.sourceTransactionId(), "occurrenceDate", e.occurrenceDate())));
        return csv.toString();
    }

    static BackupExport read(String content) {
        String[] lines = content.split("\\R");
        if (lines.length == 0 || !lines[0].startsWith("entity,")) {
            throw new IllegalArgumentException("Cabeçalho CSV inválido");
        }
        List<String> header = List.of(parseLine(lines[0]));

        List<BackupExport.ProfileData> profiles = new ArrayList<>();
        List<BackupExport.CategoryData> categories = new ArrayList<>();
        List<BackupExport.AccountData> accounts = new ArrayList<>();
        List<BackupExport.CardData> cards = new ArrayList<>();
        List<BackupExport.InvoiceData> invoices = new ArrayList<>();
        List<BackupExport.BudgetData> budgets = new ArrayList<>();
        List<BackupExport.GoalData> goals = new ArrayList<>();
        List<BackupExport.TransactionData> transactions = new ArrayList<>();
        List<BackupExport.RecurrenceExceptionData> exceptions = new ArrayList<>();

        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] values = parseLine(lines[index]);
            Map<String, String> row = new HashMap<>();
            for (int column = 0; column < header.size() && column < values.length; column++) {
                row.put(header.get(column), values[column].isEmpty() ? null : values[column]);
            }
            switch (row.get("entity")) {
                case "PROFILE" -> profiles.add(new BackupExport.ProfileData(longValue(row, "id"), row.get("name")));
                case "CATEGORY" -> categories.add(new BackupExport.CategoryData(longValue(row, "id"), row.get("name"),
                        CategoryType.valueOf(row.get("type")), Boolean.parseBoolean(row.get("archived"))));
                case "ACCOUNT" -> accounts.add(new BackupExport.AccountData(longValue(row, "id"), row.get("name"),
                        AccountType.valueOf(row.get("type")), decimal(row, "balance")));
                case "CARD" -> cards.add(new BackupExport.CardData(longValue(row, "id"), row.get("name"),
                        decimal(row, "limitAmount"), decimal(row, "availableLimit"),
                        intValue(row, "closingDay"), intValue(row, "dueDay")));
                case "INVOICE" -> invoices.add(new BackupExport.InvoiceData(longValue(row, "id"),
                        longValue(row, "creditCardId"), YearMonth.parse(row.get("billingMonth")),
                        decimal(row, "amount"), LocalDate.parse(row.get("dueDate"))));
                case "BUDGET" -> budgets.add(new BackupExport.BudgetData(longValue(row, "id"),
                        longValue(row, "categoryId"), decimal(row, "limitAmount"), YearMonth.parse(row.get("month"))));
                case "GOAL" -> goals.add(new BackupExport.GoalData(longValue(row, "id"), row.get("title"),
                        decimal(row, "targetAmount"), decimal(row, "currentAmount"), longValue(row, "profileId")));
                case "TRANSACTION" -> transactions.add(new BackupExport.TransactionData(longValue(row, "id"),
                        TransactionType.valueOf(row.get("type")), row.get("description"), longValue(row, "categoryId"),
                        decimal(row, "amount"), LocalDate.parse(row.get("date")), longValue(row, "profileId"),
                        longValue(row, "accountId"), longValue(row, "targetAccountId"), longValue(row, "creditCardId"),
                        Boolean.parseBoolean(row.get("recurring")), intValue(row, "installmentCount"),
                        longValue(row, "installmentGroupId"), longValue(row, "recurrenceSourceId")));
                case "RECURRENCE_EXCEPTION" -> exceptions.add(new BackupExport.RecurrenceExceptionData(
                        longValue(row, "sourceTransactionId"), LocalDate.parse(row.get("occurrenceDate"))));
                case null, default -> throw new IllegalArgumentException("Tipo de registro CSV inválido");
            }
        }
        return new BackupExport(profiles, categories, accounts, cards, invoices, budgets, goals, transactions, exceptions);
    }

    private static void row(StringBuilder csv, String entity, Map<String, ?> values) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        COLUMNS.forEach(column -> ordered.put(column, values.get(column)));
        ordered.put("entity", entity);
        csv.append(String.join(",", ordered.values().stream().map(BackupCsv::escape).toList())).append('\n');
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        return '"' + String.valueOf(value).replace("\"", "\"\"") + '"';
    }

    private static String[] parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString());
        return values.toArray(String[]::new);
    }

    private static Long longValue(Map<String, String> row, String column) {
        return row.get(column) == null ? null : Long.valueOf(row.get(column));
    }

    private static Integer intValue(Map<String, String> row, String column) {
        return row.get(column) == null ? null : Integer.valueOf(row.get(column));
    }

    private static BigDecimal decimal(Map<String, String> row, String column) {
        return row.get(column) == null ? null : new BigDecimal(row.get(column));
    }
}
