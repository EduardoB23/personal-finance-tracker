package com.finance.tracker.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public TransactionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE transactions ALTER COLUMN account_id DROP NOT NULL");
    }
}