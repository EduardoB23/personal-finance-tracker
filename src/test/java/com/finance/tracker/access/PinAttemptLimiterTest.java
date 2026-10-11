package com.finance.tracker.access;

import com.finance.tracker.access.PinAttemptLimiter.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PinAttemptLimiterTest {

    private static final String IP = "10.1.0.1";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-11T12:00:00Z"));
    private final PinAttemptLimiter limiter = new PinAttemptLimiter(clock);

    private Outcome wrong() {
        return limiter.attempt(IP, () -> false);
    }

    private Outcome correct() {
        return limiter.attempt(IP, () -> true);
    }

    @Test
    void blockEndsFifteenMinutesAfterFifthWrong() {
        for (int i = 0; i < 5; i++) {
            wrong();
        }
        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThat(correct()).isEqualTo(Outcome.BLOCKED);

        clock.advance(Duration.ofSeconds(1));
        assertThat(correct()).isEqualTo(Outcome.ACCEPTED);
    }

    @Test
    void attemptsWhileBlockedDoNotExtendBlock() {
        for (int i = 0; i < 5; i++) {
            wrong();
        }
        clock.advance(Duration.ofMinutes(10));
        assertThat(wrong()).isEqualTo(Outcome.BLOCKED);
        assertThat(correct()).isEqualTo(Outcome.BLOCKED);

        clock.advance(Duration.ofMinutes(5));
        assertThat(correct()).isEqualTo(Outcome.ACCEPTED);
    }

    @Test
    void correctPinResetsCount() {
        for (int i = 0; i < 4; i++) {
            assertThat(wrong()).isEqualTo(Outcome.WRONG);
        }
        assertThat(correct()).isEqualTo(Outcome.ACCEPTED);
        for (int i = 0; i < 4; i++) {
            assertThat(wrong()).isEqualTo(Outcome.WRONG);
        }
    }

    @Test
    void concurrentWrongAttemptsProduceAtMostFiveUnauthorized() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Outcome>> calls = IntStream.range(0, 10)
                    .mapToObj(i -> (Callable<Outcome>) () -> limiter.attempt(IP, () -> {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return false;
                    }))
                    .toList();
            List<Outcome> outcomes = new java.util.ArrayList<>();
            for (Future<Outcome> future : pool.invokeAll(calls)) {
                outcomes.add(future.get());
            }
            assertThat(outcomes.stream().filter(o -> o == Outcome.WRONG).count()).isLessThanOrEqualTo(5);
            assertThat(outcomes.stream().filter(o -> o == Outcome.BLOCKED).count()).isEqualTo(10 - 5);
        } finally {
            pool.shutdownNow();
        }
    }
}
