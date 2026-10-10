package com.finance.tracker.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Tipo da transação é obrigatório") TransactionType type,
        @NotBlank(message = "Descrição é obrigatória") String description,
        @NotBlank(message = "Categoria é obrigatória") String category,
        @NotNull(message = "Valor é obrigatório") @DecimalMin(value = "0.01", inclusive = true, message = "Valor deve ser maior que zero") BigDecimal amount,
        @NotNull(message = "Data é obrigatória") LocalDate date,
        @NotNull(message = "Perfil responsável é obrigatório") Long profileId,
        Long accountId,
        Long targetAccountId,
        boolean recurring,
        Integer installmentCount,
        Long creditCardId
) {
    public TransactionRequest(TransactionType type, String description, String category, BigDecimal amount,
                              LocalDate date, Long profileId, Long accountId, Long targetAccountId,
                              boolean recurring, Integer installmentCount) {
        this(type, description, category, amount, date, profileId, accountId, targetAccountId,
                recurring, installmentCount, null);
    }
}
