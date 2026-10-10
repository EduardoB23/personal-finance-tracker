package com.finance.tracker.transaction;

import jakarta.persistence.*;
import java.time.LocalDate;

// Ocorrência recorrente excluída pelo usuário: o processamento não a recria (D29)
@Entity
@Table(name = "recurrence_exceptions")
public class RecurrenceException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_transaction_id", nullable = false)
    private Long sourceTransactionId;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    public RecurrenceException() {
    }

    public RecurrenceException(Long sourceTransactionId, LocalDate occurrenceDate) {
        this.sourceTransactionId = sourceTransactionId;
        this.occurrenceDate = occurrenceDate;
    }

    public Long getSourceTransactionId() { return sourceTransactionId; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
}
