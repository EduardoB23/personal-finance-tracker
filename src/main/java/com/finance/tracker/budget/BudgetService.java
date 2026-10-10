package com.finance.tracker.budget;

import com.finance.tracker.category.Category;
import com.finance.tracker.category.CategoryRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRepository;
import com.finance.tracker.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    // Consumo da família inteira; compra no cartão conta no mês da compra e cada parcela no seu mês (D17)
    @Transactional(readOnly = true)
    public List<BudgetSummary> getBudgetSummary(YearMonth month) {
        List<Transaction> expenses = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .filter(transaction -> YearMonth.from(transaction.getDate()).equals(month))
                .toList();

        return budgetRepository.findByMonth(month).stream()
                .map(budget -> {
                    BigDecimal consumedAmount = expenses.stream()
                            .filter(transaction -> budget.getCategoryId().equals(transaction.getCategoryId()))
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal consumedPercentage = budget.getLimitAmount().signum() == 0
                            ? BigDecimal.ZERO
                            : consumedAmount.multiply(BigDecimal.valueOf(100))
                            .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
                    return new BudgetSummary(budget.getId(), budget.getCategoryId(), budget.getCategory().getName(),
                            budget.getLimitAmount(), consumedAmount, consumedPercentage, budget.getMonth());
                })
                .toList();
    }

    @Transactional
    public Budget createBudget(BudgetRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        if (budgetRepository.existsByCategory_IdAndMonth(category.getId(), request.month())) {
            throw new IllegalArgumentException("Já existe orçamento para esta categoria neste mês");
        }
        return budgetRepository.save(new Budget(category, request.limitAmount(), request.month()));
    }
}
