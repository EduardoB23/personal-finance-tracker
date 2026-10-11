package com.finance.tracker;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class H2ConsoleDisabledTest extends IntegrationTest {

    @Test
    void h2ConsoleIsNotServed() throws Exception {
        mockMvc.perform(get("/h2-console")).andExpect(status().isNotFound());
    }
}
