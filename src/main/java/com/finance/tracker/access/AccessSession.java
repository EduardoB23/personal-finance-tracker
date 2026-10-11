package com.finance.tracker.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessao")
public class AccessSession {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    protected AccessSession() {
    }

    public AccessSession(String tokenHash, LocalDateTime criadaEm, LocalDateTime expiraEm) {
        this.tokenHash = tokenHash;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }
}
