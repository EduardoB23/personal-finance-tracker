package com.finance.tracker.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/accounts")
    public List<BankAccount> getAccounts() {
        return bankAccountService.getAllAccounts();
    }

    @PostMapping("/accounts")
    public ResponseEntity<BankAccount> createAccount(@Valid @RequestBody BankAccountRequest request) {
        BankAccount created = bankAccountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
