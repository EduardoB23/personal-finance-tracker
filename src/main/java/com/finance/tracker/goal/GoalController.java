package com.finance.tracker.goal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping("/goals")
    public List<Goal> getGoals() {
        return goalService.getAllGoals();
    }

    @PostMapping("/goals")
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.createGoal(request));
    }

    @PostMapping("/goals/{goalId}/contributions")
    public Goal addContribution(@PathVariable Long goalId,
                                 @Valid @RequestBody GoalContributionRequest request) {
        return goalService.addContribution(goalId, request);
    }
}
