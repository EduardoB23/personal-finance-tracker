-- Sessões do PIN familiar (D6). Guardam o SHA-256 do cookie, nunca o token em claro.
CREATE TABLE sessao (
    token_hash VARCHAR(64) PRIMARY KEY,
    criada_em  TIMESTAMP NOT NULL,
    expira_em  TIMESTAMP NOT NULL
);
