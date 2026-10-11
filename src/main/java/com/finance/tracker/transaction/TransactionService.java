package com.finance.tracker.transaction;

import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.CreditCardInvoiceService;
import com.finance.tracker.account.CreditCardRepository;
import com.finance.tracker.category.Category;
import com.finance.tracker.category.CategoryService;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final ProfileRepository profileRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardInvoiceService creditCardInvoiceService;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository,
                              RecurrenceExceptionRepository recurrenceExceptionRepository,
                              ProfileRepository profileRepository,
                              BankAccountRepository bankAccountRepository,
                              CreditCardRepository creditCardRepository,
                              CreditCardInvoiceService creditCardInvoiceService,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.recurrenceExceptionRepository = recurrenceExceptionRepository;
        this.profileRepository = profileRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditCardInvoiceService = creditCardInvoiceService;
        this.categoryService = categoryService;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Extrato: sem parâmetros devolve tudo; com {@code month} (AAAA-MM) restringe às datas do mês,
     * do mais recente para o mais antigo; {@code profileId} restringe a uma pessoa.
     */
    public List<Transaction> getTransactions(String month, Long profileId) {
        if (month == null && profileId == null) {
            return getAllTransactions();
        }
        List<Transaction> transactions = month == null
                ? transactionRepository.findAll()
                : transactionRepository.findByDateBetween(startOfMonth(month), endOfMonth(month));
        return transactions.stream()
                .filter(transaction -> profileId == null || transaction.getProfile().getId().equals(profileId))
                .sorted(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getId).reversed())
                .toList();
    }

    private static LocalDate startOfMonth(String month) {
        return parseMonth(month).atDay(1);
    }

    private static LocalDate endOfMonth(String month) {
        return parseMonth(month).atEndOfMonth();
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mês inválido, use o formato AAAA-MM");
        }
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        int installmentCount = request.installmentCount() != null ? request.installmentCount() : 1;
        if (installmentCount <= 0) {
            throw new IllegalArgumentException("Número de parcelas deve ser maior que zero");
        }
        if (request.recurring() && installmentCount > 1) {
            throw new IllegalArgumentException("Lançamento recorrente não pode ser parcelado");
        }
        if (request.recurring() && request.type() == TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Transferências não podem ser recorrentes");
        }

        Sources sources = resolveSources(request);
        Profile profile = sources.profile();
        Category category = sources.category();
        BankAccount account = sources.account();
        CreditCard creditCard = sources.creditCard();
        BankAccount targetAccount = sources.targetAccount();

        String description = request.description().trim();
        if (installmentCount == 1) {
            Transaction transaction = new Transaction(request.type(), description, category, request.amount(),
                    request.date(), profile, account, creditCard, targetAccount);
            transaction.setRecurring(request.recurring());
            transaction = transactionRepository.save(transaction);
            applyEffect(transaction, false);
            return transaction;
        }

        // A última parcela recebe a diferença de centavos, para a soma bater com o total
        BigDecimal installmentAmount = request.amount().divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.DOWN);
        Transaction firstInstallment = null;
        for (int number = 1; number <= installmentCount; number++) {
            BigDecimal amount = number == installmentCount
                    ? request.amount().subtract(installmentAmount.multiply(BigDecimal.valueOf(installmentCount - 1L)))
                    : installmentAmount;
            Transaction installment = new Transaction(request.type(),
                    description + " - Parcela " + number + "/" + installmentCount, category, amount,
                    request.date().plusMonths(number - 1L), profile, account, creditCard, targetAccount);
            installment.setInstallmentCount(installmentCount);
            installment = transactionRepository.save(installment);
            if (firstInstallment == null) {
                firstInstallment = installment;
            }
            installment.setInstallmentGroupId(firstInstallment.getId());
            applyEffect(installment, false);
        }
        return firstInstallment;
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));

        if (transaction.getInstallmentGroupId() != null) {
            List<Transaction> installments = transactionRepository.findByInstallmentGroupId(transaction.getInstallmentGroupId());
            installments.forEach(installment -> applyEffect(installment, true));
            transactionRepository.deleteAll(installments);
            return;
        }

        applyEffect(transaction, true);
        if (transaction.getRecurrenceSourceId() != null) {
            recurrenceExceptionRepository.save(new RecurrenceException(transaction.getRecurrenceSourceId(), transaction.getDate()));
        }
        // Excluir a regra mantém as ocorrências já geradas: o banco anula recurrence_source_id delas
        transactionRepository.delete(transaction);
    }

    @Transactional
    public void processRecurringTransactions(LocalDate untilDate) {
        transactionRepository.findByRecurringTrueAndRecurrenceSourceIdIsNull()
                .forEach(rule -> generateOccurrences(rule, untilDate));
    }

    @Transactional
    public List<Transaction> generateRecurringTransactions(Long transactionId, LocalDate untilDate) {
        Transaction rule = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        if (!rule.isRecurring()) {
            throw new IllegalArgumentException("Transação não está configurada como recorrente");
        }
        return generateOccurrences(rule, untilDate);
    }

    // Gera as ocorrências mensais vencidas até a data informada, nunca depois de hoje, aplicando o efeito de cada uma
    private List<Transaction> generateOccurrences(Transaction rule, LocalDate untilDate) {
        LocalDate today = LocalDate.now();
        LocalDate limit = untilDate == null || untilDate.isAfter(today) ? today : untilDate;
        List<Transaction> generated = new ArrayList<>();

        for (int month = 1; ; month++) {
            LocalDate occurrenceDate = rule.getDate().plusMonths(month);
            if (occurrenceDate.isAfter(limit)) {
                break;
            }
            if (transactionRepository.existsByRecurrenceSourceIdAndDate(rule.getId(), occurrenceDate)
                    || recurrenceExceptionRepository.existsBySourceTransactionIdAndOccurrenceDate(rule.getId(), occurrenceDate)) {
                continue;
            }
            Transaction occurrence = new Transaction(rule.getType(), rule.getDescription() + " - Recorrência",
                    rule.getCategory(), rule.getAmount(), occurrenceDate, rule.getProfile(), rule.getAccount(),
                    rule.getCreditCard(), null);
            occurrence.setRecurrenceSourceId(rule.getId());
            occurrence = transactionRepository.save(occurrence);
            applyEffect(occurrence, false);
            generated.add(occurrence);
        }
        return generated;
    }

    private record Sources(Profile profile, Category category, BankAccount account, CreditCard creditCard,
                           BankAccount targetAccount) {
    }

    // Validação de origem, destino e categoria, igual para criar e editar
    private Sources resolveSources(TransactionRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));
        Category category = resolveCategory(request.type(), request.categoryId());

        CreditCard creditCard = null;
        BankAccount account = null;
        if (request.creditCardId() != null) {
            if (request.type() != TransactionType.EXPENSE) {
                throw new IllegalArgumentException("Cartão de crédito aceita apenas despesas");
            }
            creditCard = creditCardRepository.findById(request.creditCardId())
                    .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito não encontrado"));
        } else if (request.accountId() == null) {
            throw new IllegalArgumentException("Conta de origem é obrigatória");
        } else {
            account = bankAccountRepository.findById(request.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada"));
        }

        BankAccount targetAccount = null;
        if (request.type() == TransactionType.TRANSFER) {
            if (request.targetAccountId() == null) {
                throw new IllegalArgumentException("Transferência exige conta de destino");
            }
            targetAccount = bankAccountRepository.findById(request.targetAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada"));
            if (targetAccount.getId().equals(account.getId())) {
                throw new IllegalArgumentException("Conta de origem e destino não podem ser iguais");
            }
        }

        return new Sources(profile, category, account, creditCard, targetAccount);
    }

    // Edita um lançamento simples: estorna o efeito antigo e aplica o novo, na mesma transação
    @Transactional
    public Transaction updateTransaction(Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));
        if (transaction.getInstallmentGroupId() != null || transaction.getRecurrenceSourceId() != null
                || transaction.isRecurring()) {
            throw new IllegalArgumentException("Edição de parcelas e recorrências não é suportada");
        }
        if (request.type() != transaction.getType()) {
            throw new IllegalArgumentException("Tipo do lançamento não pode mudar");
        }
        if (request.recurring() || (request.installmentCount() != null && request.installmentCount() > 1)) {
            throw new IllegalArgumentException("Edição não altera parcelamento nem recorrência");
        }

        Sources sources = resolveSources(request);
        applyEffect(transaction, true);
        transaction.edit(request.description().trim(), sources.category(), request.amount(), request.date(),
                sources.profile(), sources.account(), sources.creditCard(), sources.targetAccount());
        applyEffect(transaction, false);
        return transaction;
    }

    private Category resolveCategory(TransactionType type, Long categoryId) {
        if (type == TransactionType.TRANSFER) {
            if (categoryId != null) {
                throw new IllegalArgumentException("Categoria inválida para este lançamento");
            }
            return null;
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Categoria é obrigatória");
        }
        return categoryService.requireForTransaction(categoryId, type);
    }

    // Efeito de um lançamento no saldo das contas ou na fatura do cartão; reverse = estorno
    private void applyEffect(Transaction transaction, boolean reverse) {
        BigDecimal amount = reverse ? transaction.getAmount().negate() : transaction.getAmount();
        switch (transaction.getType()) {
            case EXPENSE -> {
                if (transaction.getCreditCard() == null) {
                    debit(transaction.getAccount(), amount);
                } else if (reverse) {
                    creditCardInvoiceService.removePurchase(transaction.getCreditCard(), transaction.getAmount(), transaction.getDate());
                } else {
                    creditCardInvoiceService.addPurchase(transaction.getCreditCard(), transaction.getAmount(), transaction.getDate());
                }
            }
            case INCOME -> debit(transaction.getAccount(), amount.negate());
            case TRANSFER -> {
                debit(transaction.getAccount(), amount);
                debit(transaction.getTargetAccount(), amount.negate());
            }
        }
    }

    private void debit(BankAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);
    }
}
