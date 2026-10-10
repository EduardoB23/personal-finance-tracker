package com.finance.tracker.transaction;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class RecurringTransactionSchedulerTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    private Profile profile;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        profile = profileRepository.save(new Profile("Família Recorrência"));
        account = accountRepository.save(new BankAccount("Conta", AccountType.CHECKING, new BigDecimal("1000.00"), profile));
    }

    @Test
    void automaticProcessing_shouldGenerateDueOccurrenceWithoutChargingFutureBalance() {
        Transaction source = transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, "Aluguel", "Moradia", new BigDecimal("900.00"),
                LocalDate.of(2026, 1, 15), profile.getId(), account.getId(), null, true, null
        ));

        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 15));

        assertEquals(2, transactionService.getAllTransactions().size());
        assertEquals(new BigDecimal("100.00"), accountRepository.findById(account.getId()).orElseThrow().getBalance());
    }

    @Test
    void automaticProcessing_shouldBeIdempotent() {
        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, "Aluguel", "Moradia", new BigDecimal("900.00"),
                LocalDate.of(2026, 1, 15), profile.getId(), account.getId(), null, true, null
        ));

        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 15));
        transactionService.processRecurringTransactions(LocalDate.of(2026, 2, 15));

        assertEquals(2, transactionService.getAllTransactions().size());
    }
}