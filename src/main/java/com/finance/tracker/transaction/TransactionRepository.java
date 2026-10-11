package com.finance.tracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByInstallmentGroupId(Long installmentGroupId);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findByRecurringTrueAndRecurrenceSourceIdIsNull();

    boolean existsByRecurrenceSourceIdAndDate(Long recurrenceSourceId, LocalDate date);
}
