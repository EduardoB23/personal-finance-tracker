package com.finance.tracker.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String COOKIE = "FINANCE_SESSION";

    public record PinRequest(@NotBlank String pin) {
    }

    private final PinVerifier pinVerifier;
    private final PinAttemptLimiter limiter;
    private final AccessSessionService sessions;

    public AuthController(PinVerifier pinVerifier, PinAttemptLimiter limiter, AccessSessionService sessions) {
        this.pinVerifier = pinVerifier;
        this.limiter = limiter;
        this.sessions = sessions;
    }

    @PostMapping("/pin")
    public ResponseEntity<Object> login(@Valid @RequestBody PinRequest request, HttpServletRequest http) {
        PinAttemptLimiter.Outcome outcome = limiter.attempt(http.getRemoteAddr(),
                () -> pinVerifier.matches(request.pin()));
        return switch (outcome) {
            case ACCEPTED -> ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie(sessions.create()).toString())
                    .build();
            case WRONG -> ResponseEntity.status(401)
                    .body(AccessErrors.body(401, "Não autorizado", "PIN incorreto"));
            case BLOCKED -> ResponseEntity.status(429)
                    .body(AccessErrors.body(429, "Muitas requisições",
                            "Muitas tentativas. Tente novamente em 15 minutos"));
        };
    }

    private static ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(90))
                .build();
    }
}
