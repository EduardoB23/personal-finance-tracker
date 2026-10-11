package com.finance.tracker;

import com.finance.tracker.access.PinHashCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.Optional;

@SpringBootApplication
@EnableScheduling
public class FinanceApplication {

    public static void main(String[] args) {
        Optional<Integer> exitCode = PinHashCli.run(args, System.out, System.err);
        if (exitCode.isPresent()) {
            System.exit(exitCode.get());
        }
        SpringApplication.run(FinanceApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
