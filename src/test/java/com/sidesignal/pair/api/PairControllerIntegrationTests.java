package com.sidesignal.pair.api;

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

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class PairControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    PairControllerIntegrationTests(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void createAndAcceptInviteCreatesPair() throws Exception {
        // Given 2 users
        String token1 = registerAndGetToken("user1-" + UUID.randomUUID() + "@test.com", "User 1");
        String token2 = registerAndGetToken("user2-" + UUID.randomUUID() + "@test.com", "User 2");

        // User 1 creates an invite
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/pair-invites")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.inviteCode").isString())
            .andExpect(jsonPath("$.expiresAt").isString())
            .andReturn();

        JsonNode inviteNode = objectMapper.readTree(inviteResult.getResponse().getContentAsString());
        String inviteCode = inviteNode.get("inviteCode").asString();

        // User 2 accepts the invite
        mockMvc.perform(post("/api/v1/pair-invites/" + inviteCode + "/accept")
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.firstUser.displayName").value("User 1"))
            .andExpect(jsonPath("$.secondUser.displayName").value("User 2"));

        // User 1 gets current pair
        mockMvc.perform(get("/api/v1/pairs/current")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isString())
            .andExpect(jsonPath("$.firstUser.displayName").value("User 1"))
            .andExpect(jsonPath("$.secondUser.displayName").value("User 2"));
    }

    @Test
    void cannotCreateInviteIfAlreadyPaired() throws Exception {
        // Given paired users
        String token1 = registerAndGetToken("user3-" + UUID.randomUUID() + "@test.com", "User 3");
        String token2 = registerAndGetToken("user4-" + UUID.randomUUID() + "@test.com", "User 4");
        
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/pair-invites")
                .header("Authorization", "Bearer " + token1))
            .andReturn();
        String inviteCode = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("inviteCode").asString();
        
        mockMvc.perform(post("/api/v1/pair-invites/" + inviteCode + "/accept")
                .header("Authorization", "Bearer " + token2));

        // When User 1 tries to create another invite
        mockMvc.perform(post("/api/v1/pair-invites")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ALREADY_PAIRED"));
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
