package com.finance.tracker.goal;

import com.finance.tracker.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalFamilyApiTest extends IntegrationTest {

    @Test
    void goalIsFamilyWithoutProfile() throws Exception {
        mockMvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Viagem\",\"targetAmount\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileId").doesNotExist());

        mockMvc.perform(get("/api/goals"))
                .andExpect(jsonPath("$[*].title", hasItem("Viagem")));
    }

    @Test
    void contributionStillCapsAtTarget() throws Exception {
        mockMvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Carro\",\"targetAmount\":100}"))
                .andExpect(status().isCreated());
        long id = objectMapper.readTree(mockMvc.perform(get("/api/goals")).andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();

        mockMvc.perform(post("/api/goals/" + id + "/contributions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150}"))
                .andExpect(jsonPath("$.currentAmount").value(100.0));
    }
}
