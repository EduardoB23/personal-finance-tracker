package com.finance.tracker.profile;

import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(@NotBlank(message = "Nome do perfil é obrigatório") String name) {
}
