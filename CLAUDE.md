# Personal Finance Tracker

App de controle financeiro familiar: backend Spring Boot (Java 21) com H2 em arquivo e frontend React + TypeScript (Vite). Roda num PC de casa e os celulares acessam via Wi‑Fi local.

## Leia antes de mudar algo

- `docs/decisions.md` - decisões de produto e arquitetura, ordem das entregas. **Prevalece** sobre os outros documentos.
- `docs/CONTEXT.md` - padrões técnicos e convenções.
- `docs/product-spec.md` - escopo funcional.
- `.github/copilot-instructions.md` - regras completas de stack, código e processo (valem também para o Claude).

## Comandos

- Testes do backend: `mvn test` (precisa de JDK 21; use `mvn clean test` depois de mudar a versão do Java)
- Rodar o backend: `mvn spring-boot:run` (porta 8080)
- Rodar o frontend em desenvolvimento: `npm run dev` dentro de `frontend/` (proxy de `/api` para a porta 8080)

## Regras

- Código em inglês; comentários, documentação e commits em PT-BR, seguindo Conventional Commits.
- Trabalhe numa branch própria; o merge na `main` acontece depois.
- Nunca versione `data/`: é o banco H2 com dados financeiros reais. Os testes usam H2 em memória e não podem tocar nesse arquivo.
- Processo: `tlc-discover` → `tlc-plan` (`.tasks/`) → `tlc-implement` (`.checks/`). Nada em `Unresolved` é decidido durante a implementação.
