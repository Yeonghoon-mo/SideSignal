package com.sidesignal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class SideSignalApplicationTests {

    private final JdbcTemplate jdbcTemplate;
    private final MockMvc mockMvc;

    @Autowired
    SideSignalApplicationTests(JdbcTemplate jdbcTemplate, MockMvc mockMvc) {
        this.jdbcTemplate = jdbcTemplate;
        this.mockMvc = mockMvc;
    }

    @Test
    void contextLoads() {
    }

    @Test
    void flywayMigrationsCreateInitialTables() {
        List<String> tableNames = jdbcTemplate.queryForList(
            """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """,
            String.class
        );

        assertThat(tableNames)
            .contains(
                "flyway_schema_history",
                "users",
                "pairs",
                "pair_invites",
                "signals",
                "signal_events"
            );
    }

    @Test
    void actuatorHealthDoesNotRedirectToLoginPage() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void apiPathWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/v1/unknown"));
    }

}
