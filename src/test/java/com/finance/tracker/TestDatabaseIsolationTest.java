package com.finance.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Garante que nenhum teste grave no banco real em ./data/finance
@SpringBootTest
class TestDatabaseIsolationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void usesInMemoryDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:h2:mem:"), "URL do banco de teste: " + url);
        }
    }
}
