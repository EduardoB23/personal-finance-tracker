package com.finance.tracker.goal;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final ProfileRepository profileRepository;

    public GoalService(GoalRepository goalRepository, ProfileRepository profileRepository) {
        this.goalRepository = goalRepository;
        this.profileRepository = profileRepository;
    }

    public List<Goal> getAllGoals() {
        return goalRepository.findAll();
    }

    @Transactional
    public Goal createGoal(GoalRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        Goal goal = new Goal(request.title().trim(), request.targetAmount(), profile);
        profile.getGoals().add(goal);
        return goalRepository.save(goal);
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
