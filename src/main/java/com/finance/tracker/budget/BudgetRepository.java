package com.finance.tracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByMonth(YearMonth month);

    boolean existsByCategory_IdAndMonth(Long categoryId, YearMonth month);
}
