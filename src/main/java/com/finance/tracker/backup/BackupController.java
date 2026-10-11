package com.finance.tracker.backup;

import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.CreditCardInvoice;
import com.finance.tracker.account.CreditCardInvoiceRepository;
import com.finance.tracker.account.InvoicePayment;
import com.finance.tracker.account.InvoicePaymentRepository;
import com.finance.tracker.account.CreditCardRepository;
import com.finance.tracker.budget.Budget;
import com.finance.tracker.budget.BudgetRepository;
import com.finance.tracker.category.Category;
import com.finance.tracker.category.CategoryRepository;
import com.finance.tracker.goal.Goal;
import com.finance.tracker.goal.GoalRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.RecurrenceException;
import com.finance.tracker.transaction.RecurrenceExceptionRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final ProfileRepository profileRepository;
    private final CategoryRepository categoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardInvoiceRepository creditCardInvoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public BackupController(ProfileRepository profileRepository,
                            CategoryRepository categoryRepository,
                            BankAccountRepository bankAccountRepository,
                            CreditCardRepository creditCardRepository,
                            CreditCardInvoiceRepository creditCardInvoiceRepository,
                            BudgetRepository budgetRepository,
                            GoalRepository goalRepository,
                            TransactionRepository transactionRepository,
                            RecurrenceExceptionRepository recurrenceExceptionRepository,
                            InvoicePaymentRepository invoicePaymentRepository,
                            ObjectMapper objectMapper) {
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditCardInvoiceRepository = creditCardInvoiceRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.recurrenceExceptionRepository = recurrenceExceptionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> exportBackup(
            @RequestParam(defaultValue = "json") String format) throws Exception {
        boolean csv = "csv".equalsIgnoreCase(format);
        if (!csv && !"json".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Formato de backup deve ser JSON ou CSV");
        }

        BackupExport export = createExport();
        String content = csv ? BackupCsv.write(export) : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(csv ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", csv ? "finance-backup.csv" : "finance-backup.json");
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)));
    }

    private BackupExport createExport() {
        return new BackupExport(
                profileRepository.findAll().stream()
                        .map(p -> new BackupExport.ProfileData(p.getId(), p.getName())).toList(),
                categoryRepository.findAll().stream()
                        .map(c -> new BackupExport.CategoryData(c.getId(), c.getName(), c.getType(), c.isArchived())).toList(),
                bankAccountRepository.findAll().stream()
                        .map(a -> new BackupExport.AccountData(a.getId(), a.getName(), a.getType(), a.getBalance())).toList(),
                creditCardRepository.findAll().stream()
                        .map(c -> new BackupExport.CardData(c.getId(), c.getName(), c.getLimitAmount(),
                                c.getAvailableLimit(), c.getClosingDay(), c.getDueDay())).toList(),
                creditCardInvoiceRepository.findAll().stream()
                        .map(i -> new BackupExport.InvoiceData(i.getId(), i.getCreditCard().getId(), i.getBillingMonth(),
                                i.getAmount(), i.getDueDate())).toList(),
                budgetRepository.findAll().stream()
                        .map(b -> new BackupExport.BudgetData(b.getId(), b.getCategoryId(), b.getLimitAmount(), b.getMonth())).toList(),
                goalRepository.findAll().stream()
                        .map(g -> new BackupExport.GoalData(g.getId(), g.getTitle(), g.getTargetAmount(),
                                g.getCurrentAmount(), g.getProfile().getId())).toList(),
                transactionRepository.findAll().stream()
                        .map(t -> new BackupExport.TransactionData(t.getId(), t.getType(), t.getDescription(),
                                t.getCategoryId(), t.getAmount(), t.getDate(), t.getProfileId(), t.getAccountId(),
                                t.getTargetAccountId(), t.getCreditCardId(), t.isRecurring(), t.getInstallmentCount(),
                                t.getInstallmentGroupId(), t.getRecurrenceSourceId())).toList(),
                recurrenceExceptionRepository.findAll().stream()
                        .map(e -> new BackupExport.RecurrenceExceptionData(e.getSourceTransactionId(), e.getOccurrenceDate())).toList(),
                invoicePaymentRepository.findAll().stream()
                        .map(p -> new BackupExport.PaymentData(p.getId(), p.getInvoice().getCreditCard().getId(),
                                p.getInvoice().getBillingMonth(), p.getAccount().getId(), p.getProfile().getId(),
                                p.getAmount(), p.getPaidOn())).toList()
        );
    }

    // A importação substitui todo o estado local pelo conteúdo do arquivo
    @PostMapping("/import")
    @Transactional
    public ResponseEntity<Map<String, String>> importBackup(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de backup é obrigatório");
        }

        BackupExport export;
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            boolean csv = filename.endsWith(".csv") || MediaType.TEXT_PLAIN_VALUE.equals(file.getContentType())
                    || "text/csv".equals(file.getContentType());
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            export = csv ? BackupCsv.read(content) : objectMapper.readValue(content, BackupExport.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Arquivo de backup inválido ou corrompido", ex);
        }
        if (export == null || java.util.stream.Stream.of(export.profiles(), export.categories(), export.accounts(),
                export.cards(), export.invoices(), export.budgets(), export.goals(), export.transactions())
                .anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Arquivo de backup inválido ou corrompido");
        }

        // Backups antigos não têm pagamentos: a seção é opcional, as demais continuam obrigatórias
        if (export.invoicePayments() == null) {
            export = new BackupExport(export.profiles(), export.categories(), export.accounts(), export.cards(),
                    export.invoices(), export.budgets(), export.goals(), export.transactions(),
                    export.recurrenceExceptions(), java.util.List.of());
        }

        clearLocalState();
        restore(export);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Backup importado com sucesso"));
    }

    private void clearLocalState() {
        recurrenceExceptionRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        budgetRepository.deleteAllInBatch();
        goalRepository.deleteAllInBatch();
        invoicePaymentRepository.deleteAllInBatch();
        creditCardInvoiceRepository.deleteAllInBatch();
        creditCardRepository.deleteAllInBatch();
        bankAccountRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
    }

    private void restore(BackupExport export) {
        Map<Long, Profile> profiles = new HashMap<>();
        export.profiles().forEach(data -> profiles.put(data.id(), profileRepository.save(new Profile(data.name()))));

        Map<Long, Category> categories = new HashMap<>();
        export.categories().forEach(data -> {
            Category category = new Category(data.name(), data.type());
            category.setArchived(data.archived());
            categories.put(data.id(), categoryRepository.save(category));
        });

        Map<Long, BankAccount> accounts = new HashMap<>();
        export.accounts().forEach(data ->
                accounts.put(data.id(), bankAccountRepository.save(new BankAccount(data.name(), data.type(), data.balance()))));

        Map<Long, CreditCard> cards = new HashMap<>();
        export.cards().forEach(data -> cards.put(data.id(), creditCardRepository.save(new CreditCard(
                data.name(), data.limitAmount(), data.availableLimit(), data.closingDay(), data.dueDay()))));

        export.invoices().forEach(data -> creditCardInvoiceRepository.save(new CreditCardInvoice(
                required(cards, data.creditCardId(), "cartão"), data.billingMonth(), data.amount(), data.dueDate())));

        export.budgets().forEach(data -> budgetRepository.save(new Budget(
                required(categories, data.categoryId(), "categoria"), data.limitAmount(), data.month())));

        export.goals().forEach(data -> {
            Goal goal = new Goal(data.title(), data.targetAmount(), required(profiles, data.profileId(), "perfil"));
            goal.setCurrentAmount(data.currentAmount());
            goalRepository.save(goal);
        });

        Map<Long, Transaction> transactions = new HashMap<>();
        export.transactions().forEach(data -> {
            Transaction transaction = new Transaction(data.type(), data.description(),
                    data.categoryId() == null ? null : required(categories, data.categoryId(), "categoria"),
                    data.amount(), data.date(), required(profiles, data.profileId(), "perfil"),
                    data.accountId() == null ? null : required(accounts, data.accountId(), "conta"),
                    data.creditCardId() == null ? null : required(cards, data.creditCardId(), "cartão"),
                    data.targetAccountId() == null ? null : required(accounts, data.targetAccountId(), "conta"));
            transaction.setRecurring(data.recurring());
            transaction.setInstallmentCount(data.installmentCount());
            transactions.put(data.id(), transactionRepository.save(transaction));
        });

        // Os vínculos entre lançamentos usam os ids antigos e só podem ser traduzidos depois que todos existem
        export.transactions().forEach(data -> {
            Transaction transaction = transactions.get(data.id());
            if (data.installmentGroupId() != null) {
                transaction.setInstallmentGroupId(required(transactions, data.installmentGroupId(), "parcela").getId());
            }
            if (data.recurrenceSourceId() != null) {
                transaction.setRecurrenceSourceId(required(transactions, data.recurrenceSourceId(), "recorrência").getId());
            }
        });

        if (export.recurrenceExceptions() != null) {
            export.recurrenceExceptions().forEach(data -> recurrenceExceptionRepository.save(new RecurrenceException(
                    required(transactions, data.sourceTransactionId(), "recorrência").getId(), data.occurrenceDate())));
        }

        // Os saldos e limites já vêm no arquivo; aqui só voltam as linhas de pagamento
        export.invoicePayments().forEach(data -> invoicePaymentRepository.save(new InvoicePayment(
                creditCardInvoiceRepository.findByCreditCardIdAndBillingMonth(
                        required(cards, data.creditCardId(), "cartão").getId(), data.billingMonth())
                        .orElseThrow(() -> new IllegalArgumentException("Pagamento aponta para fatura inexistente")),
                required(accounts, data.accountId(), "conta"),
                required(profiles, data.profileId(), "perfil"),
                data.amount(), data.paidOn())));
    }

    private <T> T required(Map<Long, T> restored, Long oldId, String kind) {
        T value = restored.get(oldId);
        if (value == null) {
            throw new InvalidBackupException(kind + " " + oldId + " não existe no arquivo");
        }
        return value;
    }
}
