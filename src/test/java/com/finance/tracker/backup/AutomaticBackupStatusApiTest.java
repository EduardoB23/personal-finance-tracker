package com.finance.tracker.backup;

import com.finance.tracker.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutomaticBackupStatusApiTest extends IntegrationTest {

    @Test
    void statusEndpointReportsFields() throws Exception {
        mockMvc.perform(get("/api/backup/automatic-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }
}
