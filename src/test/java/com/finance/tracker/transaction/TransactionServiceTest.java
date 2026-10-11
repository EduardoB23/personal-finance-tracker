package com.finance.tracker.transaction;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.budget.Budget;
import com.finance.tracker.budget.BudgetRequest;
import com.finance.tracker.budget.BudgetService;
import com.finance.tracker.goal.Goal;
import com.finance.tracker.goal.GoalContributionRequest;
import com.finance.tracker.goal.GoalRequest;
import com.finance.tracker.goal.GoalService;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.finance.tracker.IntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionServiceTest extends IntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private GoalService goalService;

    private Profile profile;
    private BankAccount checkingAccount;
    private BankAccount savingsAccount;

    @BeforeEach
    void setUp() {
        profile = profileRepository.save(new Profile("Família Teste"));
        checkingAccount = bankAccountRepository.save(new BankAccount("Conta Corrente", AccountType.CHECKING, new BigDecimal("1000.00")));
        savingsAccount = bankAccountRepository.save(new BankAccount("Poupança", AccountType.SAVINGS, new BigDecimal("500.00")));
    }

    @Test
    void createExpense_shouldReduceAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE,
                "Supermercado",
                expenseCategoryId("Alimentação"),
                new BigDecimal("200.00"),
                LocalDate.now(),
                profile.getId(),
                checkingAccount.getId(),
                null,
                false,
                null
        );

        Transaction transaction = transactionService.createTransaction(request);

        assertNotNull(transaction.getId());
        assertEquals(new BigDecimal("800.00"), bankAccountRepository.findById(checkingAccount.getId()).orElseThrow().getBalance());
    }

    @Test
    void createTransfer_shouldDebitSourceAndCreditTarget() {
        TransactionRequest request = new TransactionRequest(
                TransactionType.TRANSFER,
                "Transferência para poupança",
                null,
                new BigDecimal("150.00"),
                LocalDate.now(),
                profile.getId(),
                checkingAccount.getId(),
                savingsAccount.getId(),
                false,
                null
        );

        transactionService.createTransaction(request);

        assertEquals(new BigDecimal("850.00"), bankAccountRepository.findById(checkingAccount.getId()).orElseThrow().getBalance());
        assertEquals(new BigDecimal("650.00"), bankAccountRepository.findById(savingsAccount.getId()).orElseThrow().getBalance());
    }

    @Test
    void createInstallmentTransaction_shouldPersistMultipleInstallments() {
        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE,
                "Compra parcelada",
                expenseCategoryId("Outros"),
                new BigDecimal("600.00"),
                LocalDate.now(),
                profile.getId(),
                checkingAccount.getId(),
                null,
                false,
                3
        );

        transactionService.createTransaction(request);

        assertEquals(3, transactionService.getAllTransactions().size());
        assertEquals(new BigDecimal("400.00"), bankAccountRepository.findById(checkingAccount.getId()).orElseThrow().getBalance());
    }

    @Test
    void generateRecurringTransactions_shouldCreateMonthlyOccurrencesAndApplyTheirEffect() {
        LocalDate startDate = LocalDate.of(2026, 1, 15);
        Transaction recurringTransaction = transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE,
                "Aluguel",
                expenseCategoryId("Moradia"),
                new BigDecimal("900.00"),
                startDate,
                profile.getId(),
                checkingAccount.getId(),
                null,
                true,
                null
        ));

        List<Transaction> generated = transactionService.generateRecurringTransactions(
                recurringTransaction.getId(),
                LocalDate.of(2026, 3, 15)
        );

        assertEquals(2, generated.size());
        assertEquals(LocalDate.of(2026, 2, 15), generated.get(0).getDate());
        assertEquals(LocalDate.of(2026, 3, 15), generated.get(1).getDate());
        // Cada ocorrência vencida aplica o seu débito (D29): 1000 - 900 x 3
        assertEquals(new BigDecimal("-1700.00"), bankAccountRepository.findById(checkingAccount.getId()).orElseThrow().getBalance());
        assertEquals(0, transactionService.generateRecurringTransactions(
            recurringTransaction.getId(),
            LocalDate.of(2026, 3, 15)
        ).size());
    }

    @Test
    void getBudgetSummary_shouldCalculateOnlyMatchingMonthlyExpenses() {
        YearMonth month = YearMonth.of(2026, 1);
        Budget budget = budgetService.createBudget(new BudgetRequest(
                expenseCategoryId("Alimentação"),
                new BigDecimal("500.00"),
                month
        ));

        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE,
                "Mercado",
                expenseCategoryId("Alimentação"),
                new BigDecimal("200.00"),
                LocalDate.of(2026, 1, 10),
                profile.getId(),
                checkingAccount.getId(),
                null,
                false,
                null
        ));

        var summary = budgetService.getBudgetSummary(month).stream()
                .filter(item -> item.id().equals(budget.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("200.00"), summary.consumedAmount());
        assertEquals(new BigDecimal("40.00"), summary.consumedPercentage());
    }

    @Test
    void addContribution_shouldTrackProgressWithoutExceedingTarget() {
        Goal goal = goalService.createGoal(new GoalRequest(
                "Viagem",
                new BigDecimal("1000.00")
        ));

        Goal updatedGoal = goalService.addContribution(
                goal.getId(),
                new GoalContributionRequest(new BigDecimal("1200.00"))
        );

        assertEquals(new BigDecimal("1000.00"), updatedGoal.getCurrentAmount());
    }
}
