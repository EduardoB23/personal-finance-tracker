package com.finance.tracker.access;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessSessionRepository extends JpaRepository<AccessSession, String> {
}
