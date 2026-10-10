# Fundação de dados (COD-11) Verification

**Verdict**: PASS - 27/27 proven
**Profile**: light (no AGENTS.md declares one) - step 1, Coverage join, Test policy and fault injection not run
**Diff range**: 0651291..HEAD (round 1 at c452136; fix commit 797a35d, fix diff c452136..797a35d)
**Round**: 2 - scoped
**Verifier**: independent sub-agent (author != verifier)

Scope of round 2: the fix diff (`BackupController.java`, `FamilyAccountsTest.java`, `BackupControllerTest.java`, `FamilyBudgetTest.java`, `RecurringTransactionSchedulerTest.java`, `TransactionDeletionTest.java`) plus every round-1 verdict that was not PASS (C8, C7, C21, C25/C26, C9, C16, and the `importBackup` NPE note). Proofs were re-run in full at 797a35d. Rows marked `carried from c452136` were untouched by the fix and keep their round-1 citation.

## Binding sources

Not run (profile `light`; no binding UI source).

## Checks

Gate run at 797a35d (`verified at 797a35d`): `JAVA_HOME="/c/Program Files/Java/jdk-21.0.11" mvn -B clean test` - exit 0, `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS. All 27 proof methods, plus `backupMissingASectionIsRejected`, appear exactly once as `<testcase>` in `target/surefire-reports/*.xml`. No report file contains `<failure>`, `<error>` or `<skipped>`.

Paths are relative to `src/test/java/com/finance/tracker/`.

| Check | Claim | Evidence | Result | Source |
|---|---|---|---|---|
| C1 | `GET /api/categories` 200, 9 defaults (7 EXPENSE, 2 INCOME), none archived | `category/CategoryApiTest.java:23-28` - `isOk()`, `hasSize(9)`, EXPENSE `containsInAnyOrder(7 names)`, INCOME `containsInAnyOrder("Salário","Outros")`, `$[*].archived everyItem(is(false))` | PASS | carried from c452136 |
| C2 | POST Pets 201; in `?type=EXPENSE`, not `?type=INCOME` | `CategoryApiTest.java:35` `isCreated()`; `:42` `hasItem("Pets")`; `:44` `not(hasItem("Pets"))` | PASS | carried from c452136 |
| C3 | case-insensitive duplicate -> 400 `Categoria já existe` | `CategoryApiTest.java:50-52` - `"alimentação"`, `isBadRequest()`, `$.message value("Categoria já existe")` | PASS | carried from c452136 |
| C4 | DELETE 204; hidden; archived=true with `includeArchived`; old transaction keeps `categoryId` | `CategoryApiTest.java:66,69,71,73` | PASS | carried from c452136 |
| C5 | nonexistent/archived/other-type -> 400 `Categoria inválida para este lançamento` | `CategoryApiTest.java:84-88` loop of 3 ids, `isBadRequest()` + message value | PASS | carried from c452136 |
| C6 | TRANSFER without category 201; EXPENSE/INCOME -> 400 `messages.categoryId` | `CategoryApiTest.java:102` `isCreated()`; `:104-109` `isBadRequest()`, `$.messages.categoryId exists()` | PASS | carried from c452136 |
| C7 | account/card created without `profileId` 201; lists carry no profile field | `account/FamilyAccountsTest.java:25,29` `isCreated()`; `:32` `$[*].name hasItem("Conjunta")`, `:36` `$[*].name hasItem("Nubank")` (lists are non-empty, so the next matchers are not vacuous); `:33-34`, `:37-38` `everyItem(not(hasKey("profile")))`, `everyItem(not(hasKey("profileId")))` | PASS (round-1 weakness closed) | verified at 797a35d |
| C8 | Maria and João post on the same account and the same card -> 201 each with author's `profileId` | `FamilyAccountsTest.java:61-62` `isCreated()`, `$.profileId value(author.getId().intValue())`; calls: Maria/account, Maria/card, João/account, and new `:55-56` João/card (`"profileId", joao.getId(), "creditCardId", nubank.getId()`) | PASS (round-1 facet gap closed) | verified at 797a35d |
| C9 | 300.00 consumed, 60.00 % | `budget/FamilyBudgetTest.java:36` `isOk()`; `:39` `content().string(containsString("\"consumedAmount\":300.00"))`; `:40` `containsString("\"consumedPercentage\":60.00")` - scale is now pinned | PASS | verified at 797a35d |
| C10 | 100 consumed in each of 2026-01/02/03 | `FamilyBudgetTest.java:53` `containsString("\"consumedAmount\":100.00")` in a loop over 3 months | PASS | verified at 797a35d |
| C11 | duplicate budget -> 400 `Já existe orçamento para esta categoria neste mês` | `FamilyBudgetTest.java` (duplicateBudgetIsRejected) - `isBadRequest()`, `$.message value("Já existe orçamento para esta categoria neste mês")`; body unchanged by the fix (round 1 cited `:121-122`; the 2 added import lines shift it by 2) | PASS | carried from c452136 |
| C12 | Feb+Mar occurrences with `recurrenceSourceId`; balance -1700.00 | `transaction/RecurringTransactionSchedulerTest.java:44-46` - dates `List.of(2026-02-15, 2026-03-15)`, `assertEquals(new BigDecimal("-1700.00"), balanceOf(account))` | PASS | carried from c452136 |
| C13 | twice -> nothing new, same balance | `RecurringTransactionSchedulerTest.java:57-58` | PASS | carried from c452136 |
| C14 | 2 entries, one per invoice, limit 1920.00 | `RecurringTransactionSchedulerTest.java:70-73` - invoices 2026-02 and 2026-03 `40.00`, `availableLimitOf(card)` `1920.00` | PASS | carried from c452136 |
| C15 | no occurrence after today | `RecurringTransactionSchedulerTest.java:84-85` | PASS | carried from c452136 |
| C16 | `ApplicationReadyEvent` generates due occurrences | `RecurringTransactionSchedulerTest.java:90-91` `ruleDate = LocalDate.now().minusMonths(2)`; `:96-97` `anyMatch(date.equals(ruleDate.plusMonths(1)))` - no longer depends on the wall clock | PASS | verified at 797a35d |
| C17 | DELETE 204, gone from GET, balance +200 | `transaction/TransactionDeletionTest.java:42,45,46` | PASS | carried from c452136 |
| C18 | income delete -> -300 | `TransactionDeletionTest.java:58` | PASS | carried from c452136 (file touched; lines above the change are unchanged) |
| C19 | transfer delete -> source +500, target -500 | `TransactionDeletionTest.java:72-73` | PASS | carried from c452136 (lines unchanged) |
| C20 | card delete -> invoice -150, limit +150 | `TransactionDeletionTest.java:85-86` | PASS | carried from c452136 (lines unchanged) |
| C21 | one installment deleted -> 3 gone, each invoice -100, limit +300 | `TransactionDeletionTest.java:96` captures `invoicesBefore` for 2026-02/03/04; `:101` `getAllTransactions().isEmpty()`; `:103` `assertEquals(invoicesBefore.get(index).subtract(new BigDecimal("100.00")), invoiceAmount(card, month))`; `:105` `limitBefore.add(new BigDecimal("300.00"))` | PASS (round-1 weakness closed: a wrong split now fails the per-invoice -100 check) | verified at 797a35d |
| C22 | deleted Feb occurrence not recreated; March kept | `TransactionDeletionTest.java` deletedOccurrenceIsNotRecreated - `before.add(900.00)`, dates `== List.of(2026-03-15)` (shifted +2 from round-1 `:116-117`) | PASS | carried from c452136 |
| C23 | rule delete reverses; generated remain; no new ones | deletingRuleStopsFutureOccurrences - `before.add(900.00)`, generated ids == remaining ids (shifted +2 from round-1 `:132,134`) | PASS | carried from c452136 |
| C24 | unknown id -> 400 `Transação não encontrada` | deletingUnknownTransactionIsRejected - `isBadRequest()`, `$.message value("Transação não encontrada")` (shifted +2 from round-1 `:140-141`) | PASS | carried from c452136 |
| C25 | JSON round trip keeps categories, references, installment group, recurrence exceptions | `backup/BackupControllerTest.java:120,129` `assertEquals(categoriesBefore, categorySnapshot())` - full sorted `type/name/archived` list; `:130-131` precondition includes `EXPENSE/Pets/false`, `EXPENSE/Antiga/true`; `:136-137` `Ração.categoryId` and budget `categoryId` equal restored ids; `:142-143` count unchanged, `noneMatch(2026-02-15)`; `:147-149` delete `Parcela 2/3` -> `noneMatch(contains("Parcela"))` | PASS (round-1 precision gap closed) | verified at 797a35d |
| C26 | same in CSV | same `roundTrip` (`:99`) with `"csv"`, `text/csv` | PASS | verified at 797a35d |
| C27 | unknown `categoryId` in backup -> 400, message contains `Backup inválido` | `BackupControllerTest.java:59-60` MockMvc multipart, `isBadRequest()`, `$.message containsString("Backup inválido")` | PASS | verified at 797a35d (lines unchanged) |

### Level (HTTP boundary)

C1-C7, C9, C11, C17, C24 and C27 assert through MockMvc. This was carried from c452136, and the C7/C9 changes stay at the MockMvc level, so there is still no level gap.

### Round-1 note: `importBackup` NPE

Closed. `src/main/java/com/finance/tracker/backup/BackupController.java:141-143` now rejects a backup when any of `profiles`, `categories`, `accounts`, `cards`, `invoices`, `budgets`, `goals` or `transactions` is null. The new test `BackupControllerTest.backupMissingASectionIsRejected` (`:64-71`) removes `"accounts": [],` and asserts `isBadRequest()` plus `$.message value("Arquivo de backup inválido ou corrompido")`. If the regex matched nothing, the import would succeed with 200 and the test would fail, so the removal is exercised. `recurrenceExceptions` stays nullable on purpose (`if (export.recurrenceExceptions() != null)`).

### Swept rows resolving to existing

Carried from c452136. `@Transactional` writes are at `TransactionService.java:52/127/147/153` and `BackupController.java:125`, and `UNIQUE (recurrence_source_id, "DATE")` is at `V1__baseline.sql:74`. The fix does not touch them; it adds 2 lines at `BackupController.java:141`, below line 125.

## Test policy rows

Not run (profile `light`).

## Faults injected

Not run (profile `light`).

## Remaining gaps

None blocking. Residual notes:

- C9/C10 now match the raw response body (`containsString("\"consumedAmount\":300.00")`) instead of `$[0]`. That is correct here because each summary holds a single budget, but it relies on Jackson's compact serialization (no space after `:`).
- C11, C22, C23 and C24 have line citations shifted by +2 from round 1 (imports added above them). Their bodies are byte-identical, and the citations above are given by method name.

## Gate

`JAVA_HOME="/c/Program Files/Java/jdk-21.0.11" mvn -B clean test` at 797a35d - 40 passed, 0 failed, 0 errors, 0 skipped (BUILD SUCCESS)
