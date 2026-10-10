package com.finance.tracker.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    @Transactional
    public BankAccount createAccount(BankAccountRequest request) {
        return bankAccountRepository.save(new BankAccount(request.name().trim(), request.type(), request.balance()));
    }
}
