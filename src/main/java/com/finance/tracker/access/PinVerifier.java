package com.finance.tracker.access;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Confere o PIN familiar contra o hash de {@code FINANCE_PIN_HASH}. A subida falha sem hash válido. */
@Component
public class PinVerifier {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String pinHash;

    public PinVerifier(@Value("${finance.pin-hash:}") String pinHash) {
        if (pinHash == null || pinHash.isBlank()) {
            throw new IllegalStateException("Defina FINANCE_PIN_HASH com o hash do PIN familiar. "
                    + "Gere com: java -jar personal-finance-tracker.jar --gerar-hash-pin=<pin>");
        }
        if (!pinHash.startsWith("$2")) {
            throw new IllegalStateException("FINANCE_PIN_HASH não é um hash BCrypt (deve começar com $2). "
                    + "Gere com: java -jar personal-finance-tracker.jar --gerar-hash-pin=<pin>");
        }
        this.pinHash = pinHash;
    }

    public boolean matches(String pin) {
        return encoder.matches(pin, pinHash);
    }
}
