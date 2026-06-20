package ru.mirea.repair;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_createsUserAndReturnsToken() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "client1@example.com",
                "fullName", "Иван Петров",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").value("client1@example.com"));
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "duplicate@example.com",
                "fullName", "Иван Петров",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidPayload_returnsValidationErrors() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "not-an-email",
                "fullName", "И",
                "password", "123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.fullName").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void login_withCorrectCredentials_returnsToken() throws Exception {
        Map<String, String> registerPayload = Map.of(
                "email", "client2@example.com",
                "fullName", "Анна Смирнова",
                "password", "password123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        Map<String, String> loginPayload = Map.of("email", "client2@example.com", "password", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void unknownPath_returnsNotFoundNotInternalServerError() throws Exception {
        Map<String, String> registerPayload = Map.of(
                "email", "probe@example.com",
                "fullName", "Тестовый Пользователь",
                "password", "password123"
        );
        var registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        // Spring 6.1+ выбрасывает NoResourceFoundException для незамапленных путей вместо
        // прямого sendError(404); без отдельного обработчика она тонет в catch-all -> 500.
        mockMvc.perform(get("/").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/robots.txt").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        Map<String, String> registerPayload = Map.of(
                "email", "client3@example.com",
                "fullName", "Пётр Сидоров",
                "password", "password123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        Map<String, String> loginPayload = Map.of("email", "client3@example.com", "password", "wrong-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isUnauthorized());
    }
}
