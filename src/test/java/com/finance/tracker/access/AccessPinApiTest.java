package com.finance.tracker.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem a configuração de sessão automática: cada requisição chega sem cookie, como de um aparelho novo.
// Cada teste usa um IP próprio, porque o limite de tentativas é um estado compartilhado do contexto.
@SpringBootTest
class AccessPinApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PinAuthFilter pinAuthFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(pinAuthFilter).build();
    }

    private static MockHttpServletRequestBuilder fromIp(MockHttpServletRequestBuilder builder, String ip) {
        return builder.with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
    }

    private MockHttpServletRequestBuilder pinRequest(String ip, String pin) {
        return fromIp(post("/api/auth/pin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pin\":\"" + pin + "\"}"), ip);
    }

    @Test
    void apiWithoutSessionReturns401WithPinMessage() throws Exception {
        mockMvc.perform(fromIp(get("/api/profiles"), "10.2.0.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("PIN necessário"));
    }

    @Test
    void correctPinReturns204AndSessionCookie() throws Exception {
        MvcResult login = mockMvc.perform(pinRequest("10.2.0.2", "2468"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("FINANCE_SESSION=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=7776000")))
                .andExpect(header().string("Set-Cookie", not(containsString("Secure"))))
                .andReturn();

        Cookie cookie = login.getResponse().getCookie("FINANCE_SESSION");
        mockMvc.perform(fromIp(get("/api/profiles"), "10.2.0.2").cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPinReturns401WithoutCookie() throws Exception {
        mockMvc.perform(pinRequest("10.2.0.3", "0000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("PIN incorreto"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void blankPinReturns400WithValidationShape() throws Exception {
        mockMvc.perform(pinRequest("10.2.0.4", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasKey("pin")));
    }

    @Test
    void fifthWrongAttemptBlocksIpEvenWithCorrectPin() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(pinRequest("10.2.0.5", "0000")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(pinRequest("10.2.0.5", "0000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("PIN incorreto"));

        mockMvc.perform(pinRequest("10.2.0.5", "2468"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Muitas tentativas. Tente novamente em 15 minutos"));
    }

    @Test
    void blockIsPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(pinRequest("10.2.0.6", "0000"));
        }
        mockMvc.perform(pinRequest("10.2.0.6", "2468")).andExpect(status().isTooManyRequests());

        mockMvc.perform(pinRequest("10.2.0.7", "2468")).andExpect(status().isNoContent());
    }

    @Test
    void tamperedCookieReturns401() throws Exception {
        mockMvc.perform(fromIp(get("/api/profiles"), "10.2.0.8")
                        .cookie(new Cookie("FINANCE_SESSION", "token-inventado")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("PIN necessário"));
    }
}
