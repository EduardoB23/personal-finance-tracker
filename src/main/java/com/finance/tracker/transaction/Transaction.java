package com.finance.tracker.transaction;

import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.category.Category;
import com.finance.tracker.profile.Profile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// No JSON, as relações aparecem só como ids (categoryId, profileId, accountId...)
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String description;

    // Nula apenas em transferências
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BankAccount account;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private BankAccount targetAccount;

    // true apenas na regra de uma recorrência; as ocorrências geradas apontam para ela em recurrenceSourceId
    @Column(nullable = false)
    private boolean recurring = false;

    @Column
    private Integer installmentCount;

    // id da parcela 1, repetido em todas as parcelas da mesma compra
    @Column(name = "installment_group_id")
    private Long installmentGroupId;

    @Column(name = "recurrence_source_id")
    private Long recurrenceSourceId;

    public Transaction() {
    }

    public Transaction(TransactionType type, String description, Category category, BigDecimal amount,
                       LocalDate date, Profile profile, BankAccount account, CreditCard creditCard,
                       BankAccount targetAccount) {
        this.type = type;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.profile = profile;
        this.account = account;
        this.creditCard = creditCard;
        this.targetAccount = targetAccount;
    }

    public Long getId() { return id; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public Profile getProfile() { return profile; }
    public BankAccount getAccount() { return account; }
    public CreditCard getCreditCard() { return creditCard; }
    public BankAccount getTargetAccount() { return targetAccount; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public void edit(String description, Category category, BigDecimal amount, LocalDate date, Profile profile,
                     BankAccount account, CreditCard creditCard, BankAccount targetAccount) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.profile = profile;
        this.account = account;
        this.creditCard = creditCard;
        this.targetAccount = targetAccount;
    }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public Long getInstallmentGroupId() { return installmentGroupId; }
    public void setInstallmentGroupId(Long installmentGroupId) { this.installmentGroupId = installmentGroupId; }
    public Long getRecurrenceSourceId() { return recurrenceSourceId; }
    public void setRecurrenceSourceId(Long recurrenceSourceId) { this.recurrenceSourceId = recurrenceSourceId; }

    public Long getCategoryId() { return category == null ? null : category.getId(); }
    public Long getProfileId() { return profile.getId(); }
    public Long getAccountId() { return account == null ? null : account.getId(); }
    public Long getCreditCardId() { return creditCard == null ? null : creditCard.getId(); }
    public Long getTargetAccountId() { return targetAccount == null ? null : targetAccount.getId(); }
}
