package com.finance.tracker.goal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public List<Goal> getAllGoals() {
        return goalRepository.findAll();
    }

    @Transactional
    public Goal createGoal(GoalRequest request) {
        return goalRepository.save(new Goal(request.title().trim(), request.targetAmount()));
    }

    @Transactional
    public Goal addContribution(Long goalId, GoalContributionRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        BigDecimal updatedAmount = goal.getCurrentAmount().add(request.amount());
        goal.setCurrentAmount(updatedAmount.min(goal.getTargetAmount()));
        return goalRepository.save(goal);
    }
}
