package com.finance.tracker.access;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Sessões do PIN familiar: 90 dias por aparelho (D6), guardadas na tabela {@code sessao}. */
@Service
public class AccessSessionService {

    static final Duration LIFETIME = Duration.ofDays(90);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccessSessionRepository repository;
    private final Clock clock;

    public AccessSessionService(AccessSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Cria uma sessão e devolve o token em claro, que só vai para o cookie. */
    @Transactional
    public String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        repository.save(new AccessSession(hash(token), now, now.plus(LIFETIME)));
        return token;
    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findById(hash(token))
                .map(session -> now.isBefore(session.getExpiraEm()))
                .orElse(false);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
