package com.finance.tracker.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Conta erros de PIN por IP (D33): 5 erros seguidos bloqueiam o IP por 15 minutos.
 * Check e contagem ficam na mesma seção {@code synchronized}, então requisições simultâneas
 * não passam todas pelo check antes de alguém incrementar.
 */
@Component
public class PinAttemptLimiter {

    static final int MAX_WRONG_ATTEMPTS = 5;
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(PinAttemptLimiter.class);

    public enum Outcome { ACCEPTED, WRONG, BLOCKED }

    private final Clock clock;
    private final Map<String, Attempts> attemptsByIp = new HashMap<>();

    public PinAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized Outcome attempt(String ip, BooleanSupplier pinMatches) {
        Attempts attempts = attemptsByIp.computeIfAbsent(ip, key -> new Attempts());
        LocalDateTime now = LocalDateTime.now(clock);
        if (attempts.blockedUntil != null) {
            if (now.isBefore(attempts.blockedUntil)) {
                return Outcome.BLOCKED;
            }
            attempts.blockedUntil = null;
            attempts.wrong = 0;
        }
        if (pinMatches.getAsBoolean()) {
            attempts.wrong = 0;
            return Outcome.ACCEPTED;
        }
        attempts.wrong++;
        if (attempts.wrong >= MAX_WRONG_ATTEMPTS) {
            attempts.blockedUntil = now.plus(BLOCK_DURATION);
            attempts.wrong = 0;
            log.warn("IP {} bloqueado por 15 minutos após 5 tentativas erradas de PIN", ip);
        }
        return Outcome.WRONG;
    }

    private static final class Attempts {
        int wrong;
        LocalDateTime blockedUntil;
    }
}
