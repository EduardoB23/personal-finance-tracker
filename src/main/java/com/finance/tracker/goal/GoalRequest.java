package com.finance.tracker.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GoalRequest(
        @NotBlank(message = "Título da meta é obrigatório") String title,
        @NotNull(message = "Valor alvo é obrigatório") @DecimalMin(value = "0.00", inclusive = true, message = "Valor alvo deve ser maior ou igual a zero") BigDecimal targetAmount
) {
}
