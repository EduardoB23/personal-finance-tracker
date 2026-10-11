package com.finance.tracker.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreditCardRequest(
        @NotBlank(message = "Nome do cartão é obrigatório") String name,
        @NotNull(message = "Limite é obrigatório") @DecimalMin(value = "0.00", inclusive = true, message = "Limite deve ser maior ou igual a zero") BigDecimal limitAmount,
        @NotNull(message = "Limite disponível é obrigatório") @DecimalMin(value = "0.00", inclusive = true, message = "Limite disponível deve ser maior ou igual a zero") BigDecimal availableLimit,
        @NotNull(message = "Dia de fechamento é obrigatório") @Min(value = 1, message = "Dia de fechamento deve ser entre 1 e 31") @Max(value = 31, message = "Dia de fechamento deve ser entre 1 e 31") Integer closingDay,
        @NotNull(message = "Dia de vencimento é obrigatório") @Min(value = 1, message = "Dia de vencimento deve ser entre 1 e 31") @Max(value = 31, message = "Dia de vencimento deve ser entre 1 e 31") Integer dueDay
) {
}
