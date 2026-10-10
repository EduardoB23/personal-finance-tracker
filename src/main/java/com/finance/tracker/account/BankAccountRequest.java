package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BankAccountRequest(
        @NotBlank(message = "Nome da conta é obrigatório") String name,
        @NotNull(message = "Tipo da conta é obrigatório") AccountType type,
        @NotNull(message = "Saldo inicial é obrigatório") @DecimalMin(value = "0.00", inclusive = true, message = "Saldo inicial deve ser maior ou igual a zero") BigDecimal balance
) {
}
