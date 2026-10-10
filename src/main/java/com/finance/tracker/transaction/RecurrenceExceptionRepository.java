package com.finance.tracker.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    boolean existsBySourceTransactionIdAndOccurrenceDate(Long sourceTransactionId, LocalDate occurrenceDate);
}
