package com.sidesignal.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.sidesignal.TestcontainersConfiguration;
import com.sidesignal.auth.infrastructure.UserRepository;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class AuthControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Autowired
    AuthControllerIntegrationTests(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        UserRepository userRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @Test
    void registerCreatesUserAndReturnsAccessToken() throws Exception {
        String email = testEmail();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123!",
                      "displayName": "영훈"
                    }
                    """.formatted(email.toUpperCase())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.expiresIn").value(7200))
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.displayName").value("영훈"))
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(userRepository.findByEmail(email))
            .hasValueSatisfying(user -> {
                assertThat(user.getId()).isEqualTo(UUID.fromString(response.get("user").get("id").asString()));
                assertThat(user.getPasswordHash()).isNotEqualTo("password123!");
            });
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        String email = testEmail();
        register(email, "password123!", "로그인");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123!"
                    }
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String email = testEmail();
        register(email, "password123!", "중복");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123!",
                      "displayName": "중복"
                    }
                    """.formatted(email)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        String email = testEmail();
        register(email, "password123!", "실패");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "wrong-password"
                    }
                    """.formatted(email)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenReturnsAuthenticationEntryPointResponse() throws Exception {
        mockMvc.perform(get("/api/v1/pairs/current")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/v1/pairs/current"));
    }

    @Test
    void authenticatedUnknownApiPathReturnsCommonNotFoundResponse() throws Exception {
        String email = testEmail();
        String accessToken = register(email, "password123!", "인증").get("accessToken").asString();

        mockMvc.perform(get("/api/v1/unknown")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.path").value("/api/v1/unknown"));
    }

    private JsonNode register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s",
                      "displayName": "%s"
                    }
                    """.formatted(email, password, displayName)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String testEmail() {
        return "user-" + UUID.randomUUID() + "@sidesignal.test";
    }

}
