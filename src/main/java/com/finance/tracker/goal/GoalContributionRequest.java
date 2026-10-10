package com.finance.tracker.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GoalContributionRequest(
        @NotNull(message = "Valor do aporte é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor do aporte deve ser maior que zero")
        BigDecimal amount
) {
}