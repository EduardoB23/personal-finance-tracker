package com.finance.tracker;

import com.finance.tracker.account.AccountType;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.BankAccountRepository;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.CreditCardInvoiceRepository;
import com.finance.tracker.account.CreditCardRepository;
import com.finance.tracker.category.Category;
import com.finance.tracker.category.CategoryRepository;
import com.finance.tracker.category.CategoryType;
import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.Transaction;
import com.finance.tracker.transaction.TransactionRequest;
import com.finance.tracker.transaction.TransactionService;
import com.finance.tracker.transaction.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

// Base dos testes de integração: cada teste roda numa transação desfeita ao final, sobre o H2 em memória
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected EntityManager entityManager;
    @Autowired protected ProfileRepository profileRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected BankAccountRepository bankAccountRepository;
    @Autowired protected CreditCardRepository creditCardRepository;
    @Autowired protected CreditCardInvoiceRepository invoiceRepository;
    @Autowired protected TransactionService transactionService;

    protected Profile profile(String name) {
        return profileRepository.save(new Profile(name));
    }

    protected BankAccount account(String name, String balance) {
        return bankAccountRepository.save(new BankAccount(name, AccountType.CHECKING, new BigDecimal(balance)));
    }

    // Fecha no dia 20 e vence no dia 10: compra até o dia 20 cai na fatura do mês seguinte
    protected CreditCard card(String name, String limit) {
        return creditCardRepository.save(new CreditCard(name, new BigDecimal(limit), new BigDecimal(limit), 20, 10));
    }

    protected Category category(String name, CategoryType type) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals(name) && category.getType() == type)
                .findFirst()
                .orElseThrow();
    }

    protected Long expenseCategoryId(String name) {
        return category(name, CategoryType.EXPENSE).getId();
    }

    protected Transaction accountExpense(Profile profile, BankAccount account, String amount, LocalDate date) {
        return transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE, "Despesa",
                expenseCategoryId("Alimentação"), new BigDecimal(amount), date, profile.getId(), account.getId(),
                null, false, null));
    }

    protected Transaction cardExpense(Profile profile, CreditCard card, String amount, LocalDate date, Integer installments) {
        return transactionService.createTransaction(new TransactionRequest(TransactionType.EXPENSE, "Compra",
                expenseCategoryId("Alimentação"), new BigDecimal(amount), date, profile.getId(), null, null,
                false, installments, card.getId()));
    }

    protected BigDecimal balanceOf(BankAccount account) {
        return bankAccountRepository.findById(account.getId()).orElseThrow().getBalance();
    }

    protected BigDecimal availableLimitOf(CreditCard card) {
        return creditCardRepository.findById(card.getId()).orElseThrow().getAvailableLimit();
    }

    protected BigDecimal invoiceAmount(CreditCard card, YearMonth billingMonth) {
        return invoiceRepository.findByCreditCardIdAndBillingMonth(card.getId(), billingMonth).orElseThrow().getAmount();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
