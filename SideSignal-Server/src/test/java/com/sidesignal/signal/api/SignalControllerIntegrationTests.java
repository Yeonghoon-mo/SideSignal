package com.sidesignal.signal.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import com.sidesignal.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class SignalControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;
    private String user3Token; // Unpaired user

    @Autowired
    SignalControllerIntegrationTests(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() throws Exception {
        user1Token = registerAndGetToken("s-user1-" + UUID.randomUUID() + "@test.com", "User 1");
        user2Token = registerAndGetToken("s-user2-" + UUID.randomUUID() + "@test.com", "User 2");
        user3Token = registerAndGetToken("s-user3-" + UUID.randomUUID() + "@test.com", "User 3");

        // Pair User 1 and User 2
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/pair-invites")
                .header("Authorization", "Bearer " + user1Token))
            .andReturn();
        String inviteCode = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("inviteCode").asString();
        
        mockMvc.perform(post("/api/v1/pair-invites/" + inviteCode + "/accept")
                .header("Authorization", "Bearer " + user2Token));
    }

    @Test
    void unpairedUserCannotAccessSignalApi() throws Exception {
        mockMvc.perform(get("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user3Token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PAIR_NOT_FOUND"));
    }

    @Test
    void getOrCreateMySignalReturnsOfflineStatusInitially() throws Exception {
        mockMvc.perform(get("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OFFLINE"))
            .andExpect(jsonPath("$.departureTime").isEmpty())
            .andExpect(jsonPath("$.message").isEmpty());
    }

    @Test
    void updateMySignalUpdatesStatusAndDepartureTime() throws Exception {
        String now = "2026-04-23T18:00:00Z";
        
        mockMvc.perform(patch("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "FOCUSING",
                      "departureTime": "%s",
                      "message": "Do not disturb"
                    }
                    """.formatted(now)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FOCUSING"))
            .andExpect(jsonPath("$.departureTime").value(now))
            .andExpect(jsonPath("$.message").value("Do not disturb"));
    }

    @Test
    void clearDepartureTimeRemovesIt() throws Exception {
        // Set departure time
        mockMvc.perform(patch("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "LEAVING_SOON",
                      "departureTime": "2026-04-23T18:00:00Z"
                    }
                    """));

        // Clear departure time
        mockMvc.perform(delete("/api/v1/me/signal/departure-time")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isNoContent());

        // Verify it's null
        mockMvc.perform(get("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.departureTime").isEmpty());
    }

    @Test
    void getPairSignalsReturnsBothSignals() throws Exception {
        mockMvc.perform(patch("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"FOCUSING\"}"));

        mockMvc.perform(patch("/api/v1/me/signal")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"COFFEE_AVAILABLE\"}"));

        mockMvc.perform(get("/api/v1/pairs/current/signals")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signals").isArray())
            .andExpect(jsonPath("$.signals.length()").value(2));
    }

    private String registerAndGetToken(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "password123!",
                      "displayName": "%s"
                    }
                    """.formatted(email, displayName)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }
}
