package com.finance.tracker.transaction;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.finance.tracker.IntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallmentRoundingTest extends IntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    void installmentAmount_shouldDistributeRemainderInLastInstallment() {
        Profile profile = profileRepository.save(new Profile("Família Parcelas"));
        BankAccount account = accountRepository.save(new BankAccount("Conta", AccountType.CHECKING, new BigDecimal("1000.00")));

        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, "Compra", expenseCategoryId("Lazer"), new BigDecimal("100.00"),
                LocalDate.of(2026, 1, 10), profile.getId(), account.getId(), null, false, 3
        ));

        List<Transaction> transactions = transactionService.getAllTransactions();
        assertEquals(new BigDecimal("33.33"), transactions.get(0).getAmount());
        assertEquals(new BigDecimal("33.33"), transactions.get(1).getAmount());
        assertEquals(new BigDecimal("33.34"), transactions.get(2).getAmount());
        assertEquals(new BigDecimal("900.00"), accountRepository.findById(account.getId()).orElseThrow().getBalance());
    }
}