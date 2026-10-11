package com.finance.tracker.access;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.PrintStream;
import java.util.Optional;

/**
 * Comando {@code --gerar-hash-pin=<pin>}: imprime o hash BCrypt do PIN familiar para ser gravado em
 * {@code FINANCE_PIN_HASH}. Roda antes do Spring, então o servidor não sobe.
 */
public final class PinHashCli {

    static final String FLAG = "--gerar-hash-pin=";

    private PinHashCli() {
    }

    /** Devolve o código de saída quando o comando foi pedido, ou vazio para subir o servidor. */
    public static Optional<Integer> run(String[] args, PrintStream out, PrintStream err) {
        for (String arg : args) {
            if (arg.startsWith(FLAG)) {
                String pin = arg.substring(FLAG.length());
                if (pin.isBlank()) {
                    err.println("Uso: --gerar-hash-pin=<pin>");
                    return Optional.of(1);
                }
                out.println(new BCryptPasswordEncoder().encode(pin));
                return Optional.of(0);
            }
        }
        return Optional.empty();
    }
}
