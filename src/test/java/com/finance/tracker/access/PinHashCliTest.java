package com.finance.tracker.access;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PinHashCliTest {

    @Test
    void printsBcryptHashAndReturnsZero() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Optional<Integer> code = PinHashCli.run(new String[]{"--gerar-hash-pin=1234"},
                new PrintStream(out), new PrintStream(err));

        String printed = out.toString().trim();
        assertThat(code).contains(0);
        assertThat(printed).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("1234", printed)).isTrue();
    }

    @Test
    void blankPinReturnsOneWithUsage() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Optional<Integer> code = PinHashCli.run(new String[]{"--gerar-hash-pin="},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(err));

        assertThat(code).contains(1);
        assertThat(err.toString()).contains("Uso: --gerar-hash-pin=<pin>");
    }

    @Test
    void withoutFlagServerStarts() {
        Optional<Integer> code = PinHashCli.run(new String[]{"--server.port=8081"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));

        assertThat(code).isEmpty();
    }
}
