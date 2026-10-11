package com.finance.tracker.account;

import com.finance.tracker.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationApiTest extends IntegrationTest {

    @Test
    void profileCreatedAppearsInList() throws Exception {
        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maria\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/profiles"))
                .andExpect(jsonPath("$[*].name", hasItem("Maria")));
    }

    @Test
    void accountCreatedAppearsInList() throws Exception {
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conjunta\",\"type\":\"CHECKING\",\"balance\":100.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts"))
                .andExpect(jsonPath("$[*].name", hasItem("Conjunta")));
    }

    @Test
    void cardCreatedAppearsInList() throws Exception {
        mockMvc.perform(post("/api/credit-cards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nubank\",\"limitAmount\":3000,\"availableLimit\":3000,"
                                + "\"closingDay\":20,\"dueDay\":10}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/credit-cards"))
                .andExpect(jsonPath("$[*].name", hasItem("Nubank")));
    }

    @Test
    void cardDayOutsideOneToThirtyOneIsRejected() throws Exception {
        mockMvc.perform(post("/api/credit-cards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nubank\",\"limitAmount\":3000,\"availableLimit\":3000,"
                                + "\"closingDay\":0,\"dueDay\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages.closingDay").exists());

        mockMvc.perform(post("/api/credit-cards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nubank\",\"limitAmount\":3000,\"availableLimit\":3000,"
                                + "\"closingDay\":20,\"dueDay\":32}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages.dueDay").exists());
    }
}
