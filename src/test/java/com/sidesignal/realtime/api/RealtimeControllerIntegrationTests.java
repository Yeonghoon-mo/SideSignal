package com.sidesignal.realtime.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class RealtimeControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    RealtimeControllerIntegrationTests(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void subscribeReturnsSseEmitterAndConnectEvent() throws Exception {
        String token1 = registerAndGetToken("sse1-" + UUID.randomUUID() + "@test.com");
        String token2 = registerAndGetToken("sse2-" + UUID.randomUUID() + "@test.com");

        // 페어 연결
        MvcResult inviteResult = mockMvc.perform(post("/api/v1/pair-invites")
                        .header("Authorization", "Bearer " + token1))
                .andReturn();
        String inviteCode = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("inviteCode").asString();
        mockMvc.perform(post("/api/v1/pair-invites/" + inviteCode + "/accept")
                .header("Authorization", "Bearer " + token2));

        // SSE 구독 시도
        MvcResult sseResult = mockMvc.perform(get("/api/v1/pairs/current/events")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andReturn();

        String sseContent = sseResult.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(sseContent).contains("event:connect");
        org.assertj.core.api.Assertions.assertThat(sseContent).contains("data:connected");
    }

    @Test
    void unpairedUserCannotSubscribe() throws Exception {
        String token = registerAndGetToken("sse3-" + UUID.randomUUID() + "@test.com");

        mockMvc.perform(get("/api/v1/pairs/current/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123!",
                                  "displayName": "User"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }
}
