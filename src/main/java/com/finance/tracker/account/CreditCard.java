package com.finance.tracker.account;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Cartão da família: qualquer perfil pode lançar nele (D28)
@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private BigDecimal availableLimit;

    @Column(nullable = false)
    private Integer closingDay;

    @Column(nullable = false)
    private Integer dueDay;

    public CreditCard() {
    }

    public CreditCard(String name, BigDecimal limitAmount, BigDecimal availableLimit, Integer closingDay, Integer dueDay) {
        this.name = name;
        this.limitAmount = limitAmount;
        this.availableLimit = availableLimit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public BigDecimal getAvailableLimit() { return availableLimit; }
    public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
    public Integer getClosingDay() { return closingDay; }
    public void setClosingDay(Integer closingDay) { this.closingDay = closingDay; }
    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
}
