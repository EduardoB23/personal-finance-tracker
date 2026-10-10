package com.finance.tracker.budget;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetSummary(
        Long id,
        String category,
        BigDecimal limitAmount,
        BigDecimal consumedAmount,
        BigDecimal consumedPercentage,
        YearMonth month
) {
}