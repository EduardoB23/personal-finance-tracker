package com.finance.tracker.budget;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRepository;
import com.finance.tracker.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ProfileRepository profileRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         ProfileRepository profileRepository,
                         TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public List<BudgetSummary> getBudgetSummary(YearMonth month) {
        List<Transaction> transactions = transactionRepository.findAll();

        return budgetRepository.findAll().stream()
            .filter(budget -> budget.getMonth().equals(month))
            .map(budget -> {
                BigDecimal consumedAmount = transactions.stream()
                    .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                    .filter(transaction -> transaction.getDate() != null
                        && YearMonth.from(transaction.getDate()).equals(month))
                    .filter(transaction -> transaction.getCategory().equalsIgnoreCase(budget.getCategory()))
                    .filter(transaction -> transaction.getProfile().getId().equals(budget.getProfile().getId()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal consumedPercentage = budget.getLimitAmount().signum() == 0
                    ? BigDecimal.ZERO
                    : consumedAmount.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);

                return new BudgetSummary(
                    budget.getId(),
                    budget.getCategory(),
                    budget.getLimitAmount(),
                    consumedAmount,
                    consumedPercentage,
                    budget.getMonth()
                );
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public Budget createBudget(BudgetRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        Budget budget = new Budget(request.category().trim(), request.limitAmount(), request.month(), profile);
        profile.getBudgets().add(budget);
        return budgetRepository.save(budget);
    }
}
