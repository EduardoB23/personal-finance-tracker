package com.finance.tracker.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {
    Optional<CreditCardInvoice> findByCreditCardIdAndBillingMonth(Long creditCardId, YearMonth billingMonth);
}