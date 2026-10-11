package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// Pagamento de uma fatura: debita a conta, devolve o limite do cartão e não é lançamento (D18)
@Entity
@Table(name = "invoice_payments")
public class InvoicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CreditCardInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    public InvoicePayment() {
    }

    public InvoicePayment(CreditCardInvoice invoice, BankAccount account, Profile profile, BigDecimal amount, LocalDate paidOn) {
        this.invoice = invoice;
        this.account = account;
        this.profile = profile;
        this.amount = amount;
        this.paidOn = paidOn;
    }

    public Long getId() { return id; }
    public CreditCardInvoice getInvoice() { return invoice; }
    public BankAccount getAccount() { return account; }
    public Profile getProfile() { return profile; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaidOn() { return paidOn; }
}
