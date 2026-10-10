package com.finance.tracker.budget;

import com.finance.tracker.category.Category;
import com.finance.tracker.common.YearMonthAttributeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.YearMonth;

// Orçamento da família por categoria e mês (D16)
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "budget_month", nullable = false)
    private YearMonth month;

    public Budget() {
    }

    public Budget(Category category, BigDecimal limitAmount, YearMonth month) {
        this.category = category;
        this.limitAmount = limitAmount;
        this.month = month;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public Long getCategoryId() { return category.getId(); }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public YearMonth getMonth() { return month; }
}
