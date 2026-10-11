package com.finance.tracker;

import com.finance.tracker.access.AccessSessionService;
import com.finance.tracker.access.AuthController;
import jakarta.servlet.http.Cookie;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Faz cada requisição dos testes de integração sair de um aparelho já autenticado: uma sessão real
 * é criada na tabela {@code sessao} e seu cookie vai em todas as chamadas. Os testes do PIN montam
 * o MockMvc sem essa configuração, para exercitar o login de verdade.
 */
@Configuration
class TestAuthConfiguration {

    @Bean
    MockMvcBuilderCustomizer authenticatedFamilyDevice(AccessSessionService sessions) {
        return builder -> builder.defaultRequest(get("/")
                .cookie(new Cookie(AuthController.COOKIE, sessions.create())));
    }
}
