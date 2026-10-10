package com.finance.tracker.account;

import com.finance.tracker.common.YearMonthAttributeConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "credit_card_invoices")
public class CreditCardInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "billing_month", nullable = false)
    private YearMonth billingMonth;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate dueDate;

    public CreditCardInvoice() {
    }

    public CreditCardInvoice(CreditCard creditCard, YearMonth billingMonth, BigDecimal amount, LocalDate dueDate) {
        this.creditCard = creditCard;
        this.billingMonth = billingMonth;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CreditCard getCreditCard() { return creditCard; }
    public YearMonth getBillingMonth() { return billingMonth; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
}