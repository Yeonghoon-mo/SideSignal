package com.sidesignal.poke.api;

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
import tools.jackson.databind.ObjectMapper;

import com.sidesignal.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class PokeControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    PokeControllerIntegrationTests(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void pokeSendsResponseAndSseEvent() throws Exception {
        PairedUsers users = createPairedUsers();

        MvcResult sseResult = mockMvc.perform(get("/api/v1/pairs/current/events")
                .header("Authorization", "Bearer " + users.recipientToken()))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(post("/api/v1/pokes")
                .header("Authorization", "Bearer " + users.senderToken()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recipientDisplayName").value("Receiver"))
            .andExpect(jsonPath("$.sentAt").isString());

        String sseContent = sseResult.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(sseContent).contains("event:connect");
    }

    @Test
    void pokeIsRateLimitedForTenSeconds() throws Exception {
        PairedUsers users = createPairedUsers();

        mockMvc.perform(post("/api/v1/pokes")
                .header("Authorization", "Bearer " + users.senderToken()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pokes")
                .header("Authorization", "Bearer " + users.senderToken()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("POKE_COOLDOWN"));
    }

    @Test
    void unpairedUserCannotPoke() throws Exception {
        String token = registerAndGetToken("poke-unpaired-" + UUID.randomUUID() + "@test.com", "Solo");

        mockMvc.perform(post("/api/v1/pokes")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PAIR_NOT_FOUND"));
    }

    private PairedUsers createPairedUsers() throws Exception {
        String senderToken = registerAndGetToken("poke-sender-" + UUID.randomUUID() + "@test.com", "Sender");
        String recipientToken = registerAndGetToken("poke-recipient-" + UUID.randomUUID() + "@test.com", "Receiver");

        MvcResult inviteResult = mockMvc.perform(post("/api/v1/pair-invites")
                .header("Authorization", "Bearer " + senderToken))
            .andReturn();
        String inviteCode = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("inviteCode").asString();

        mockMvc.perform(post("/api/v1/pair-invites/" + inviteCode + "/accept")
                .header("Authorization", "Bearer " + recipientToken))
            .andExpect(status().isCreated());

        return new PairedUsers(senderToken, recipientToken);
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

    private record PairedUsers(
        String senderToken,
        String recipientToken
    ) {
    }
}
