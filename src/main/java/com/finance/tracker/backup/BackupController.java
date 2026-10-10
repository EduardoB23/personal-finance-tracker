package com.finance.tracker.backup;

import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.account.CreditCardRepository;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.CreditCardInvoice;
import com.finance.tracker.account.CreditCardInvoiceRepository;
import com.finance.tracker.budget.Budget;
import com.finance.tracker.budget.BudgetRepository;
import com.finance.tracker.goal.Goal;
import com.finance.tracker.goal.GoalRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final ProfileRepository profileRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardInvoiceRepository creditCardInvoiceRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public BackupController(ProfileRepository profileRepository,
                           BankAccountRepository bankAccountRepository,
                           CreditCardRepository creditCardRepository,
                           CreditCardInvoiceRepository creditCardInvoiceRepository,
                           BudgetRepository budgetRepository,
                           GoalRepository goalRepository,
                           TransactionRepository transactionRepository,
                           ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditCardInvoiceRepository = creditCardInvoiceRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportBackup(
            @RequestParam(defaultValue = "json") String format) throws Exception {
        BackupExport export = createExport();
        boolean csv = "csv".equalsIgnoreCase(format);
        if (!csv && !"json".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Formato de backup deve ser JSON ou CSV");
        }

        String content = csv ? toCsv(export) : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(csv ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", csv ? "finance-backup.csv" : "finance-backup.json");
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private BackupExport createExport() {
        return new BackupExport(
            profileRepository.findAll().stream().map(profile ->
                new BackupExport.ProfileData(profile.getId(), profile.getName())).toList(),
            bankAccountRepository.findAll().stream().map(account ->
                new BackupExport.AccountData(account.getId(), account.getName(), account.getType(),
                    account.getBalance(), account.getProfile().getId())).toList(),
            creditCardRepository.findAll().stream().map(card ->
                new BackupExport.CardData(card.getId(), card.getName(), card.getLimitAmount(),
                    card.getAvailableLimit(), card.getClosingDay(), card.getDueDay(), card.getProfile().getId())).toList(),
            creditCardInvoiceRepository.findAll().stream().map(invoice ->
                new BackupExport.InvoiceData(invoice.getId(), invoice.getCreditCard().getId(), invoice.getBillingMonth(),
                    invoice.getAmount(), invoice.getDueDate())).toList(),
            budgetRepository.findAll().stream().map(budget ->
                new BackupExport.BudgetData(budget.getId(), budget.getCategory(), budget.getLimitAmount(),
                    budget.getMonth(), budget.getProfile().getId())).toList(),
            goalRepository.findAll().stream().map(goal ->
                new BackupExport.GoalData(goal.getId(), goal.getTitle(), goal.getTargetAmount(),
                    goal.getCurrentAmount(), goal.getProfile().getId())).toList(),
            transactionRepository.findAll().stream().map(transaction ->
                new BackupExport.TransactionData(transaction.getId(), transaction.getType(), transaction.getDescription(),
                    transaction.getCategory(), transaction.getAmount(), transaction.getDate(), transaction.getProfile().getId(),
                    transaction.getAccount() == null ? null : transaction.getAccount().getId(), transaction.getTargetAccount() == null ? null : transaction.getTargetAccount().getId(),
                    transaction.getCreditCard() == null ? null : transaction.getCreditCard().getId(),
                    transaction.isRecurring(), transaction.getInstallmentCount())).toList()
        );
    }

    @PostMapping("/import")
    @Transactional
    public ResponseEntity<Map<String, String>> importBackup(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de backup é obrigatório");
        }

        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            boolean csv = filename.endsWith(".csv") || MediaType.TEXT_PLAIN_VALUE.equals(file.getContentType())
                    || "text/csv".equals(file.getContentType());
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            BackupExport export = csv ? fromCsv(content) : objectMapper.readValue(content, BackupExport.class);
            if (export == null) {
                throw new IllegalArgumentException("Conteúdo do backup inválido");
            }

            transactionRepository.deleteAllInBatch();
            transactionRepository.flush();
            budgetRepository.deleteAllInBatch();
            budgetRepository.flush();
            goalRepository.deleteAllInBatch();
            goalRepository.flush();
            creditCardInvoiceRepository.deleteAllInBatch();
            creditCardInvoiceRepository.flush();
            creditCardRepository.deleteAllInBatch();
            creditCardRepository.flush();
            bankAccountRepository.deleteAllInBatch();
            bankAccountRepository.flush();
            profileRepository.deleteAllInBatch();
            profileRepository.flush();
            entityManager.clear();

            Map<Long, Profile> profiles = new HashMap<>();
            for (BackupExport.ProfileData data : export.profiles()) {
                Profile profile = new Profile(data.name());
                profiles.put(data.id(), profileRepository.save(profile));
            }
            profileRepository.flush();

            Map<Long, BankAccount> accounts = new HashMap<>();
            Map<Long, CreditCard> cards = new HashMap<>();
            for (BackupExport.AccountData data : export.accounts()) {
                BankAccount account = new BankAccount(data.name(), data.type(), data.balance(), requiredProfile(profiles, data.profileId()));
                accounts.put(data.id(), bankAccountRepository.save(account));
            }
            bankAccountRepository.flush();

            for (BackupExport.CardData data : export.cards()) {
                CreditCard card = new CreditCard(data.name(), data.limitAmount(), data.availableLimit(),
                        data.closingDay(), data.dueDay(), requiredProfile(profiles, data.profileId()));
                cards.put(data.id(), creditCardRepository.save(card));
                }
            creditCardRepository.flush();

                for (BackupExport.InvoiceData data : export.invoices()) {
                CreditCardInvoice invoice = new CreditCardInvoice(requiredCard(cards, data.creditCardId()), data.billingMonth(),
                    data.amount(), data.dueDate());
                creditCardInvoiceRepository.save(invoice);
            }

            for (BackupExport.BudgetData data : export.budgets()) {
                Budget budget = new Budget(data.category(), data.limitAmount(), data.month(), requiredProfile(profiles, data.profileId()));
                budgetRepository.save(budget);
            }

            for (BackupExport.GoalData data : export.goals()) {
                Goal goal = new Goal(data.title(), data.targetAmount(), requiredProfile(profiles, data.profileId()));
                goal.setCurrentAmount(data.currentAmount());
                goalRepository.save(goal);
            }

            for (BackupExport.TransactionData data : export.transactions()) {
                Transaction transaction = new Transaction(data.type(), data.description(), data.category(), data.amount(), data.date(),
                    requiredProfile(profiles, data.profileId()), data.accountId() == null ? null : requiredAccount(accounts, data.accountId()),
                        data.targetAccountId() == null ? null : requiredAccount(accounts, data.targetAccountId()),
                        data.recurring(), data.installmentCount());
                if (data.creditCardId() != null) {
                    transaction.setCreditCard(requiredCard(cards, data.creditCardId()));
                }
                transactionRepository.save(transaction);
            }

            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Backup importado com sucesso"));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Arquivo de backup inválido ou corrompido", ex);
        }
    }

    private String toCsv(BackupExport export) {
        StringBuilder csv = new StringBuilder("entity,id,profileId,accountId,targetAccountId,name,type,description,category,amount,balance,limitAmount,availableLimit,closingDay,dueDay,month,title,targetAmount,currentAmount,date,recurring,installmentCount,creditCardId\n");
        export.profiles().forEach(item -> row(csv, "PROFILE", item.id(), null, null, null, item.name(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        export.accounts().forEach(item -> row(csv, "ACCOUNT", item.id(), item.profileId(), null, null, item.name(), item.type(), null, null, null, item.balance(), null, null, null, null, null, null, null, null, null, null));
        export.cards().forEach(item -> row(csv, "CARD", item.id(), item.profileId(), null, null, item.name(), null, null, null, null, null, item.limitAmount(), item.availableLimit(), item.closingDay(), item.dueDay(), null, null, null, null, null, null));
        export.invoices().forEach(item -> row(csv, "INVOICE", item.id(), null, null, null, null, null, null, item.amount(), null, null, null, null, null, item.billingMonth(), null, null, null, item.dueDate(), null, null, item.creditCardId()));
        export.budgets().forEach(item -> row(csv, "BUDGET", item.id(), item.profileId(), null, null, null, null, item.category(), null, null, item.limitAmount(), null, null, null, item.month(), null, null, null, null, null, null));
        export.goals().forEach(item -> row(csv, "GOAL", item.id(), item.profileId(), null, null, null, null, null, null, null, null, null, null, null, null, item.title(), item.targetAmount(), item.currentAmount(), null, null, null));
        export.transactions().forEach(item -> row(csv, "TRANSACTION", item.id(), item.profileId(), item.accountId(), item.targetAccountId(), null, item.type(), item.description(), item.category(), item.amount(), null, null, null, null, null, null, null, null, item.date(), item.recurring(), item.installmentCount(), item.creditCardId()));
        return csv.toString();
    }

    private void row(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(',');
            csv.append(escape(values[index]));
        }
        csv.append('\n');
    }

    private String escape(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private BackupExport fromCsv(String content) {
        List<BackupExport.ProfileData> profiles = new ArrayList<>();
        List<BackupExport.AccountData> accounts = new ArrayList<>();
        List<BackupExport.CardData> cards = new ArrayList<>();
        List<BackupExport.InvoiceData> invoices = new ArrayList<>();
        List<BackupExport.BudgetData> budgets = new ArrayList<>();
        List<BackupExport.GoalData> goals = new ArrayList<>();
        List<BackupExport.TransactionData> transactions = new ArrayList<>();

        String[] lines = content.split("\\R");
        if (lines.length < 2 || !lines[0].startsWith("entity,id,profileId")) {
            throw new IllegalArgumentException("Cabeçalho CSV inválido");
        }
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) continue;
            String[] columns = parseCsvLine(lines[index]);
            if (columns.length > 23) throw new IllegalArgumentException("Linha CSV inválida");
            columns = Arrays.copyOf(columns, 23);
            switch (columns[0]) {
                case "PROFILE" -> profiles.add(new BackupExport.ProfileData(longValue(columns[1]), text(columns[5])));
                case "ACCOUNT" -> accounts.add(new BackupExport.AccountData(longValue(columns[1]), text(columns[5]),
                        enumValue(com.finance.tracker.account.AccountType.class, columns[6]), decimal(columns[10]), longValue(columns[2])));
                case "CARD" -> cards.add(new BackupExport.CardData(longValue(columns[1]), text(columns[5]), decimal(columns[11]), decimal(columns[12]),
                        integerValue(columns[13]), integerValue(columns[14]), longValue(columns[2])));
                case "INVOICE" -> {
                    int dueDateIndex = isBoolean(columns[19]) ? 18 : 19;
                    int cardIndex = dueDateIndex + 3;
                    invoices.add(new BackupExport.InvoiceData(longValue(columns[1]), longValue(columns[cardIndex]),
                            java.time.YearMonth.parse(text(columns[15])), decimal(columns[9]), java.time.LocalDate.parse(text(columns[dueDateIndex]))));
                }
                case "BUDGET" -> budgets.add(new BackupExport.BudgetData(longValue(columns[1]), text(columns[8]), decimal(columns[11]),
                        java.time.YearMonth.parse(text(columns[15])), longValue(columns[2])));
                case "GOAL" -> goals.add(new BackupExport.GoalData(longValue(columns[1]), text(columns[16]), decimal(columns[17]), decimal(columns[18]), longValue(columns[2])));
                case "TRANSACTION" -> {
                    int dateIndex = isBoolean(columns[19]) ? 18 : 19;
                    transactions.add(new BackupExport.TransactionData(longValue(columns[1]),
                        enumValue(com.finance.tracker.transaction.TransactionType.class, columns[6]), text(columns[7]), text(columns[8]), decimal(columns[9]),
                        java.time.LocalDate.parse(text(columns[dateIndex])), longValue(columns[2]), longValue(columns[3]), longValue(columns[4]),
                        longValue(columns[dateIndex + 3]), Boolean.parseBoolean(text(columns[dateIndex + 1])), integerValue(columns[dateIndex + 2])));
                }
                default -> throw new IllegalArgumentException("Tipo de registro CSV inválido");
            }
        }
        return new BackupExport(profiles, accounts, cards, invoices, budgets, goals, transactions);
    }

    private String[] parseCsvLine(String line) {
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

    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private String text(String value) { return value == null || value.isBlank() ? null : value; }
    private Long longValue(String value) { return text(value) == null ? null : Long.valueOf(value); }
    private Integer integerValue(String value) { return text(value) == null ? null : Integer.valueOf(value); }
    private BigDecimal decimal(String value) { return text(value) == null ? null : new BigDecimal(value); }
    private <T extends Enum<T>> T enumValue(Class<T> type, String value) { return Enum.valueOf(type, value); }

    private Profile requiredProfile(Map<Long, Profile> profiles, Long profileId) {
        Profile profile = profiles.get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Backup referencia um perfil inexistente");
        }
        return profile;
    }

    private BankAccount requiredAccount(Map<Long, BankAccount> accounts, Long accountId) {
        BankAccount account = accounts.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Backup referencia uma conta inexistente");
        }
        return account;
    }

    private CreditCard requiredCard(Map<Long, CreditCard> cards, Long cardId) {
        CreditCard card = cards.get(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Backup referencia um cartão inexistente");
        }
        return card;
    }
}
