package com.finance.tracker.access;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
class PinLogTest {

    private static final String PIN = "2468";
    // Hash configurado em src/test/resources/application.properties
    private static final String HASH = "$2a$10$L1bVrQO/V45qhiVmwQgVweOW2Jv0gM6B4W4HsoIpQ5K9SskSByNrq";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PinAuthFilter pinAuthFilter;

    private Logger accessLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        accessLogger = (Logger) LoggerFactory.getLogger("com.finance.tracker.access");
        appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        accessLogger.detachAppender(appender);
    }

    @Test
    void neverLogsPinOrHash() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(pinAuthFilter).build();

        // Cinco erros bloqueiam o IP e disparam o log de bloqueio; depois um acerto no mesmo IP
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(attempt("10.3.0.1", "0000"));
        }
        mockMvc.perform(attempt("10.3.0.1", PIN));

        assertThat(appender.list).isNotEmpty();
        for (ILoggingEvent event : appender.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(PIN).doesNotContain(HASH);
        }
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder attempt(String ip, String pin) {
        return post("/api/auth/pin").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"" + pin + "\"}");
    }
}
