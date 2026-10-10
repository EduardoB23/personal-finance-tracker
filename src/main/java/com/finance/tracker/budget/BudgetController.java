package com.finance.tracker.budget;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/budgets")
    public List<Budget> getBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/budgets/summary")
    public List<BudgetSummary> getBudgetSummary(@RequestParam YearMonth month) {
        return budgetService.getBudgetSummary(month);
    }

    @PostMapping("/budgets")
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request));
    }
}
