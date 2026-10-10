# Entrega 1 - Base: Java 21 e instruções de agentes alinhadas - Verification

**Verdict**: PASS
**Profile**: light (no AGENTS.md declares one) - step 1, Coverage join, Test policy and fault injection not run by profile
**Diff range**: 4936ff3..4e15d21 (HEAD)
**Round**: 1 - full
**Verifier**: independent sub-agent (author != verifier)

## Binding sources

Step 1 not run (profile `light`, no binding UI source). Task criteria cross-checked against the checks instead:

| Task criterion (`.tasks/base-java21-docs.md`) | Covered by |
|---|---|
| 1 - 15 existing tests pass on JDK 21 | C1 (16 = 15 + new isolation test; adjustment recorded at `.checks/base-java21-docs.md:30`) |
| 2 - bytecode version 65 | C2 |
| 3 - no Linear/mcp_linear/.agent/, tlc flow with .tasks/.checks | C3, C4 |
| 4 - stack Java 21, package, Long, Flyway, decisions.md prevails | C5 |
| 5 - CLAUDE.md contents | C6 |
| 6 - data/finance.mv.db untouched, in-memory H2 | C7, C8 |

All 6 criteria are covered.

## Checks

| Check | Claim | Proof run | Evidence | Result |
|---|---|---|---|---|
| C1 | 16 tests, 0 failures, 0 errors on JDK 21 | `JAVA_HOME=jdk-21.0.11 mvn -B clean test` exit 0 | log: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`; surefire XML lists 16 testcases incl. `TestDatabaseIsolationTest.usesInMemoryDatabase` | PASS |
| C2 | classes are major version 65 | `javap -v target/classes/com/finance/tracker/FinanceApplication.class \| grep "major version: 65"` (after the clean build) exit 0 | output `major version: 65`; `pom.xml:18` `<java.version>21</java.version>` | PASS |
| C3 | no `Linear`, `mcp_linear`, `.agent/` | `! grep -nE "Linear\|mcp_linear\|\.agent/" .github/copilot-instructions.md` exit 0 | grep returned no lines (exit 1, negated to 0) | PASS |
| C4 | tlc flow with `.tasks/` and `.checks/` | literal proof exit 0 | `.github/copilot-instructions.md:54` tlc-discover, `:55` tlc-plan + `.tasks/<nome>.md`, `:56` tlc-implement + `.checks/<nome>.md` | PASS |
| C5 | Java 21, `com.finance.tracker`, `Long`, Flyway, decisions.md prevails | literal proof exit 0 | `.github/copilot-instructions.md:14` Java 21, `:16` `com.finance.tracker`, `:17` `Long`, `:18` Flyway, `:5` decisions.md prevalecem. Consistent with `docs/decisions.md:27-30` (D9-D12) | PASS |
| C6 | CLAUDE.md at root with the listed content | literal proof exit 0 | `CLAUDE.md:7` decisions.md, `:8` CONTEXT.md, `:9` product-spec.md, `:14` `mvn test`, `:15` `mvn spring-boot:run`, `:16` `npm run dev` in `frontend/`, `:20` inglês / PT-BR / Conventional Commits, `:22` nunca versione `data/` | PASS |
| C7 | test DataSource URL is `jdbc:h2:mem:` | `JAVA_HOME=jdk-21.0.11 mvn -B test -Dtest=TestDatabaseIsolationTest` exit 0, `Tests run: 1, Failures: 0` in `TestDatabaseIsolationTest` | `src/test/java/com/finance/tracker/TestDatabaseIsolationTest.java:24` - `assertTrue(url.startsWith("jdbc:h2:mem:"), ...)` with `url = connection.getMetaData().getURL()` (`:23`); runtime log `HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:finance-test` | PASS (see gap 1) |
| C8 | `mvn test` does not modify `data/finance.mv.db` | literal proof: mvn exit 0, compare exit 0; also checked around the `clean test` run | before = after = `9408fd27...c3221e` mtime `1791637526`, both runs | PASS |

### Config sanity (task criterion 6 / Landing row)

`src/test/resources/application.properties:4` contains only `spring.datasource.url=jdbc:h2:mem:finance-test;DB_CLOSE_DELAY=-1` (lines 1-3 are comments). Main `src/main/resources/application.yml` still applies in tests, evidenced in the test log: app name `personal-finance-tracker` (yml:6) in every log line, 166 `Hibernate:` SQL lines (`show-sql: true`, yml:16), `create table bank_accounts (...)` from `ddl-auto: update` (yml:15), H2Dialect warning (yml:13). `data/` is not tracked (`git ls-files data` empty).

## Gaps (ranked)

1. **Precision gap (checklist), low**: C7's proof `mvn -B test -Dtest=TestDatabaseIsolationTest` does not set `JAVA_HOME`. Run literally in this environment (default JAVA_HOME = JDK 17) it fails with exit 1 (`class file version 65.0 ... only recognizes up to 61.0`, `Tests run: 0`). It passes with `JAVA_HOME` on JDK 21, as C1/C8 proofs set. Not a code defect; the proof line should carry the same `JAVA_HOME=` prefix.
2. **Observation, not a finding**: `.github/copilot-instructions.md:18` says the schema evolves via Flyway, "nunca com `ddl-auto: update`", while `application.yml:15` still uses `ddl-auto: update`. This matches D12 and Flyway is explicitly out of scope (entrega 2), so it is a documented future state, not a contradiction of `docs/decisions.md`.

## Gate

`JAVA_HOME=jdk-21.0.11 mvn -B clean test` - 16 passed, 0 failed, 0 errors (BUILD SUCCESS)
