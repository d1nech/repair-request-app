package ru.mirea.repair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.entity.User;
import ru.mirea.repair.repository.UserRepository;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepairRequestSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "password123";

    private String clientToken;
    private String otherClientToken;
    private String adminToken;
    private String operatorToken;
    private String master1Token;
    private String master2Token;
    private Long master1Id;
    private Long clientUserId;

    @BeforeEach
    void setUp() throws Exception {
        clientUserId = createUser("client@example.com", "Иван Клиент", Role.USER).getId();
        createUser("other-client@example.com", "Пётр Чужой", Role.USER);
        createUser("admin@test.com", "Админ Системы", Role.ADMIN);
        createUser("operator@test.com", "Оператор Системы", Role.OPERATOR);
        master1Id = createUser("master1@test.com", "Мастер Первый", Role.MASTER).getId();
        createUser("master2@test.com", "Мастер Второй", Role.MASTER);

        clientToken = login("client@example.com");
        otherClientToken = login("other-client@example.com");
        adminToken = login("admin@test.com");
        operatorToken = login("operator@test.com");
        master1Token = login("master1@test.com");
        master2Token = login("master2@test.com");
    }

    private User createUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    private String login(String email) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", RAW_PASSWORD))))
                .andExpect(status().isOk());
        JsonNode body = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private Long createRequestAs(String token) throws Exception {
        Map<String, Object> payload = Map.of(
                "title", "Не работает принтер",
                "description", "Принтер не печатает документы, нужна диагностика.",
                "equipmentType", "Принтер",
                "location", "Кабинет 204",
                "priority", "MEDIUM"
        );
        ResultActions result = mockMvc.perform(post("/api/requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
        JsonNode body = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    void anonymousRequestToProtectedEndpoint_isRejected() throws Exception {
        // Точный код (401 или 403) зависит от конфигурации AuthenticationEntryPoint в Spring Security;
        // важна сама блокировка неавторизованного доступа.
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void clientSeesOnlyOwnRequests() throws Exception {
        createRequestAs(clientToken);

        mockMvc.perform(get("/api/requests").header("Authorization", "Bearer " + otherClientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/requests").header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void strangerCannotReadOrCommentOnSomeoneElsesRequest() throws Exception {
        Long requestId = createRequestAs(clientToken);

        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + otherClientToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/requests/" + requestId + "/comments")
                .header("Authorization", "Bearer " + otherClientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("message", "Можно перенести заявку?"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotChangeStatus_adminAndOperatorCan() throws Exception {
        Long requestId = createRequestAs(clientToken);

        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                .header("Authorization", "Bearer " + clientToken)
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                .header("Authorization", "Bearer " + operatorToken)
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .param("status", "WAITING_PARTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PARTS"));
    }

    @Test
    void operatorAssignsMaster_onlyAssignedMasterCanChangeStatus() throws Exception {
        Long requestId = createRequestAs(clientToken);

        mockMvc.perform(patch("/api/requests/" + requestId + "/assign")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("masterId", master1Id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedMasterEmail").value("master1@test.com"));

        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                .header("Authorization", "Bearer " + master2Token)
                .param("status", "DONE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/requests/" + requestId + "/status")
                .header("Authorization", "Bearer " + master1Token)
                .param("status", "DONE")
                .param("comment", "Ремонт выполнен"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/requests/" + requestId + "/status-history")
                .header("Authorization", "Bearer " + master1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.newStatus == 'DONE')]").exists());
    }

    @Test
    void assigningNonMasterUser_returnsBadRequest() throws Exception {
        Long requestId = createRequestAs(clientToken);

        mockMvc.perform(patch("/api/requests/" + requestId + "/assign")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("masterId", clientUserId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientCannotAssignMasterOrClassify() throws Exception {
        Long requestId = createRequestAs(clientToken);

        mockMvc.perform(patch("/api/requests/" + requestId + "/assign")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("masterId", master1Id))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/requests/" + requestId + "/classify")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("categoryId", 1))))
                .andExpect(status().isForbidden());
    }
}
