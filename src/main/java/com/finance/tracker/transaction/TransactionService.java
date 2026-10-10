package com.finance.tracker.transaction;

import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.CreditCardInvoiceService;
import com.finance.tracker.account.CreditCardRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProfileRepository profileRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardInvoiceService creditCardInvoiceService;

    public TransactionService(TransactionRepository transactionRepository,
                             ProfileRepository profileRepository,
                             BankAccountRepository bankAccountRepository,
                             CreditCardRepository creditCardRepository,
                             CreditCardInvoiceService creditCardInvoiceService) {
        this.transactionRepository = transactionRepository;
        this.profileRepository = profileRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditCardInvoiceService = creditCardInvoiceService;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional
    public void processRecurringTransactions(LocalDate untilDate) {
        transactionRepository.findAll().stream()
                .filter(Transaction::isRecurring)
                .forEach(transaction -> generateRecurringTransactions(transaction.getId(), untilDate));
    }

    @Transactional
    public List<Transaction> generateRecurringTransactions(Long transactionId, LocalDate untilDate) {
        Transaction sourceTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada"));

        if (!sourceTransaction.isRecurring()) {
            throw new IllegalArgumentException("Transação não está configurada como recorrente");
        }
        if (sourceTransaction.getType() == TransactionType.TRANSFER) {
            throw new IllegalArgumentException("Transferências não podem ser recorrentes");
        }
        if (untilDate == null || !untilDate.isAfter(sourceTransaction.getDate())) {
            return List.of();
        }

        List<Transaction> existingTransactions = transactionRepository.findAll();
        List<Transaction> generatedTransactions = new ArrayList<>();
        LocalDate occurrenceDate = nextMonthlyDate(sourceTransaction.getDate());

        while (!occurrenceDate.isAfter(untilDate)) {
            LocalDate currentOccurrenceDate = occurrenceDate;
            boolean alreadyGenerated = existingTransactions.stream().anyMatch(transaction ->
                    transaction.getProfile().getId().equals(sourceTransaction.getProfile().getId())
                            && transaction.getAccount().getId().equals(sourceTransaction.getAccount().getId())
                            && transaction.getType() == sourceTransaction.getType()
                            && transaction.getCategory().equals(sourceTransaction.getCategory())
                            && transaction.getAmount().compareTo(sourceTransaction.getAmount()) == 0
                            && transaction.getDate().equals(currentOccurrenceDate)
                            && transaction.getDescription().equals(recurringDescription(sourceTransaction))
            );

            if (!alreadyGenerated) {
                Transaction generatedTransaction = new Transaction(
                        sourceTransaction.getType(),
                        recurringDescription(sourceTransaction),
                        sourceTransaction.getCategory(),
                        sourceTransaction.getAmount(),
                        currentOccurrenceDate,
                        sourceTransaction.getProfile(),
                        sourceTransaction.getAccount(),
                        null,
                        false,
                        null
                );
                generatedTransactions.add(transactionRepository.save(generatedTransaction));
            }

            occurrenceDate = nextMonthlyDate(occurrenceDate);
        }

        return generatedTransactions;
    }

    private String recurringDescription(Transaction sourceTransaction) {
        return sourceTransaction.getDescription() + " - Recorrência";
    }

    private LocalDate nextMonthlyDate(LocalDate date) {
        YearMonth nextMonth = YearMonth.from(date).plusMonths(1);
        return nextMonth.atDay(Math.min(date.getDayOfMonth(), nextMonth.lengthOfMonth()));
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        int installmentCount = request.installmentCount() != null ? request.installmentCount() : 1;

        if (installmentCount <= 0) {
            throw new IllegalArgumentException("Número de parcelas deve ser maior que zero");
        }

        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        BankAccount sourceAccount = request.accountId() == null ? null : bankAccountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada"));

        CreditCard creditCard = null;
        if (request.creditCardId() != null) {
            if (request.type() != TransactionType.EXPENSE) {
                throw new IllegalArgumentException("Cartão de crédito aceita apenas despesas");
            }
            creditCard = creditCardRepository.findById(request.creditCardId())
                    .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito não encontrado"));
            if (!creditCard.getProfile().getId().equals(profile.getId())) {
                throw new IllegalArgumentException("Cartão não pertence ao perfil informado");
            }
        } else if (sourceAccount == null) {
            throw new IllegalArgumentException("Conta de origem é obrigatória");
        }

        if (sourceAccount != null && !sourceAccount.getProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("Conta não pertence ao perfil informado");
        }

        BankAccount targetAccount = null;
        if (request.targetAccountId() != null) {
            targetAccount = bankAccountRepository.findById(request.targetAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada"));
            if (!targetAccount.getProfile().getId().equals(profile.getId())) {
                throw new IllegalArgumentException("Conta de destino não pertence ao perfil informado");
            }
        }

        BigDecimal amount = request.amount();
        List<Transaction> createdTransactions = new java.util.ArrayList<>();

        if (installmentCount > 1) {
            BigDecimal installmentAmount = amount.divide(BigDecimal.valueOf(installmentCount), 2, java.math.RoundingMode.DOWN);

            for (int i = 1; i <= installmentCount; i++) {
                BigDecimal currentAmount = i == installmentCount
                        ? amount.subtract(installmentAmount.multiply(BigDecimal.valueOf(installmentCount - 1L)))
                        : installmentAmount;
                Transaction installment = creditCard == null ? new Transaction(
                        request.type(),
                        request.description().trim() + " - Parcela " + i + "/" + installmentCount,
                        request.category().trim(),
                        currentAmount,
                        request.date().plusMonths(i - 1L),
                        profile,
                        sourceAccount,
                        targetAccount,
                        request.recurring(),
                        installmentCount
                ) : new Transaction(request.type(), request.description().trim() + " - Parcela " + i + "/" + installmentCount,
                    request.category().trim(), currentAmount, request.date().plusMonths(i - 1L), profile,
                    null, null, request.recurring(), installmentCount);
                if (creditCard != null) {
                    installment.setCreditCard(creditCard);
                }

                if (request.type() == TransactionType.EXPENSE) {
                    if (creditCard == null) {
                        sourceAccount.setBalance(sourceAccount.getBalance().subtract(currentAmount));
                    } else {
                        creditCardInvoiceService.addPurchase(creditCard, currentAmount, request.date().plusMonths(i - 1L));
                    }
                } else if (request.type() == TransactionType.INCOME) {
                    sourceAccount.setBalance(sourceAccount.getBalance().add(currentAmount));
                } else if (request.type() == TransactionType.TRANSFER) {
                    if (targetAccount == null) {
                        throw new IllegalArgumentException("Transferência exige conta de destino");
                    }
                    sourceAccount.setBalance(sourceAccount.getBalance().subtract(currentAmount));
                    targetAccount.setBalance(targetAccount.getBalance().add(currentAmount));
                }

                profile.getTransactions().add(installment);
                createdTransactions.add(transactionRepository.save(installment));
            }

            if (request.type() == TransactionType.TRANSFER) {
                bankAccountRepository.save(sourceAccount);
                bankAccountRepository.save(targetAccount);
            } else if (creditCard == null) {
                bankAccountRepository.save(sourceAccount);
            }

            return createdTransactions.get(0);
        }

        if (request.type() == TransactionType.TRANSFER) {
            if (targetAccount == null) {
                throw new IllegalArgumentException("Transferência exige conta de destino");
            }
            if (sourceAccount.getId().equals(targetAccount.getId())) {
                throw new IllegalArgumentException("Conta de origem e destino não podem ser iguais");
            }

            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            targetAccount.setBalance(targetAccount.getBalance().add(amount));
            bankAccountRepository.save(sourceAccount);
            bankAccountRepository.save(targetAccount);
        } else if (creditCard == null) {
            if (request.type() == TransactionType.EXPENSE) {
                sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            } else if (request.type() == TransactionType.INCOME) {
                sourceAccount.setBalance(sourceAccount.getBalance().add(amount));
            }
            bankAccountRepository.save(sourceAccount);
        }

        Transaction transaction = creditCard == null ? new Transaction(
                request.type(),
                request.description().trim(),
                request.category().trim(),
                amount,
                request.date(),
                profile,
                sourceAccount,
                targetAccount,
                request.recurring(),
                request.installmentCount()
            ) : new Transaction(request.type(), request.description().trim(), request.category().trim(), amount,
                request.date(), profile, null, null, request.recurring(), request.installmentCount());
            if (creditCard != null) {
                transaction.setCreditCard(creditCard);
            }

            if (creditCard != null) {
                creditCardInvoiceService.addPurchase(creditCard, amount, request.date());
            }

        profile.getTransactions().add(transaction);
        return transactionRepository.save(transaction);
    }
}
