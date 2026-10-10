package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import com.finance.tracker.transaction.TransactionRequest;
import com.finance.tracker.transaction.TransactionService;
import com.finance.tracker.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.finance.tracker.IntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditCardBillingTest extends IntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private CreditCardInvoiceRepository invoiceRepository;

    @Autowired
    private TransactionService transactionService;

    private Profile profile;
    private CreditCard card;

    @BeforeEach
    void setUp() {
        profile = profileRepository.save(new Profile("Família Cartão"));
        card = creditCardService.createCard(new CreditCardRequest(
                "Cartão Principal",
                new BigDecimal("2000.00"),
                new BigDecimal("2000.00"),
                20,
                10
        ));
    }

    @Test
    void cardPurchase_shouldCreateDueMonthInvoiceAndReduceAvailableLimit() {
        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE,
                "Compra no cartão",
                expenseCategoryId("Alimentação"),
                new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10),
                profile.getId(),
                null,
                null,
                false,
                null,
                card.getId()
        ));

        CreditCardInvoice invoice = invoiceRepository.findByCreditCardIdAndBillingMonth(card.getId(), YearMonth.of(2026, 2))
                .orElseThrow();

        assertEquals(new BigDecimal("150.00"), invoice.getAmount());
        assertEquals(new BigDecimal("1850.00"), card.getAvailableLimit());
    }

    @Test
    void cardPurchasesInSameDueMonth_shouldBeConsolidated() {
        createPurchase("Compra 1", new BigDecimal("100.00"));
        createPurchase("Compra 2", new BigDecimal("75.00"));

        assertEquals(1, invoiceRepository.findAll().size());
        assertEquals(new BigDecimal("175.00"), invoiceRepository.findAll().get(0).getAmount());
    }

    private void createPurchase(String description, BigDecimal amount) {
        transactionService.createTransaction(new TransactionRequest(
                TransactionType.EXPENSE, description, expenseCategoryId("Alimentação"), amount,
                LocalDate.of(2026, 1, 10), profile.getId(), null, null, false, null, card.getId()
        ));
    }
}