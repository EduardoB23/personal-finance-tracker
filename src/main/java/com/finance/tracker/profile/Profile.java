package com.finance.tracker.profile;

import com.finance.tracker.goal.Goal;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

// Membro da família. Contas, cartões e orçamentos são da família (D16, D28); o perfil só identifica quem lançou
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JsonManagedReference("profile-goals")
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Goal> goals = new ArrayList<>();

    public Profile() {
    }

    public Profile(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Goal> getGoals() { return goals; }
    public void setGoals(List<Goal> goals) { this.goals = goals; }
}
