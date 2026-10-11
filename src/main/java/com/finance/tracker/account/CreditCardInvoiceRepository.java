package com.finance.tracker.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {

    // Lock pessimista: pagamento, estorno e compras da mesma fatura passam uma de cada vez
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CreditCardInvoice> findByCreditCardIdAndBillingMonth(Long creditCardId, YearMonth billingMonth);

    List<CreditCardInvoice> findByCreditCardIdOrderByBillingMonthAsc(Long creditCardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CreditCardInvoice i where i.id = :id")
    Optional<CreditCardInvoice> lockById(@Param("id") Long id);
}
