package com.finance.tracker.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "Nome da categoria é obrigatório") String name,
        @NotNull(message = "Tipo da categoria é obrigatório") CategoryType type
) {
}
