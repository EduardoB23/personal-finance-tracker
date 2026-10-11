package com.finance.tracker.access;

import com.finance.tracker.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccessSessionServiceTest extends IntegrationTest {

    @Autowired
    private AccessSessionRepository repository;

    @Test
    void sessionSurvivesServiceRecreation() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-11T12:00:00Z"));
        String token = new AccessSessionService(repository, clock).create();

        AccessSessionService recreated = new AccessSessionService(repository, clock);
        assertThat(recreated.isValid(token)).isTrue();
        assertThat(repository.count()).isGreaterThan(0);
    }

    @Test
    void sessionExpiresAfterNinetyDays() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-11T12:00:00Z"));
        AccessSessionService service = new AccessSessionService(repository, clock);
        String token = service.create();

        clock.advance(Duration.ofDays(90).minusMinutes(1));
        assertThat(service.isValid(token)).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(service.isValid(token)).isFalse();
    }
}
