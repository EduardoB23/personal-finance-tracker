package com.finance.tracker.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetRequest(
        @NotNull(message = "Categoria do orçamento é obrigatória") Long categoryId,
        @NotNull(message = "Limite do orçamento é obrigatório") @DecimalMin(value = "0.00", inclusive = true, message = "Limite deve ser maior ou igual a zero") BigDecimal limitAmount,
        @NotNull(message = "Mês do orçamento é obrigatório") YearMonth month
) {
}
