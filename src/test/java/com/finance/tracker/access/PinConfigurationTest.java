package com.finance.tracker.access;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinConfigurationTest {

    @Test
    void missingHashFailsStartupWithInstructions() {
        assertThatThrownBy(() -> new PinVerifier(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Defina FINANCE_PIN_HASH")
                .hasMessageContaining("--gerar-hash-pin=<pin>");
    }

    @Test
    void hashWithoutBcryptPrefixFailsStartup() {
        assertThatThrownBy(() -> new PinVerifier("2468"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não é um hash BCrypt");
    }
}
