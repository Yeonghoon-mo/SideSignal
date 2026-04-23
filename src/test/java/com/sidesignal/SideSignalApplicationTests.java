package com.sidesignal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SideSignalApplicationTests {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SideSignalApplicationTests(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

}
