package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import com.ginga.naviai.dashboard.service.DashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardActivityIntegrationTest {

    private EmbeddedDatabase db;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setup() {
        db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        jdbcTemplate = new NamedParameterJdbcTemplate(db);
        KnowledgeRepository kr = Mockito.mock(KnowledgeRepository.class);
        dashboardService = new DashboardServiceImpl(kr, jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        db.shutdown();
    }

    @Test
    void getActivity_ShouldAggregateCounts_FromRealSql() {
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS knowledge (id BIGINT PRIMARY KEY, created_at TIMESTAMP, is_deleted BOOLEAN);");
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS comment (id BIGINT PRIMARY KEY, created_at TIMESTAMP, is_deleted BOOLEAN);");
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS \"like\" (id BIGINT PRIMARY KEY, created_at TIMESTAMP);");

        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE knowledge;");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE comment;");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE \"like\";");

        jdbcTemplate.getJdbcTemplate().update("INSERT INTO knowledge (id, created_at, is_deleted) VALUES (1, ?, false)", new Object[]{Timestamp.valueOf(LocalDateTime.of(2026,2,8,10,0))});
        jdbcTemplate.getJdbcTemplate().update("INSERT INTO knowledge (id, created_at, is_deleted) VALUES (2, ?, false)", new Object[]{Timestamp.valueOf(LocalDateTime.of(2026,2,10,12,0))});

        jdbcTemplate.getJdbcTemplate().update("INSERT INTO comment (id, created_at, is_deleted) VALUES (1, ?, false)", new Object[]{Timestamp.valueOf(LocalDateTime.of(2026,2,8,11,0))});

        jdbcTemplate.getJdbcTemplate().update("INSERT INTO \"like\" (id, created_at) VALUES (1, ?)", new Object[]{Timestamp.valueOf(LocalDateTime.of(2026,2,9,9,0))});

        LocalDate from = LocalDate.of(2026,2,8);
        LocalDate to = LocalDate.of(2026,2,10);

        ActivityResponse resp = dashboardService.getActivity(from, to, "week");

        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(3);

        assertThat(resp.getItems().get(0).getDate()).isEqualTo(LocalDate.of(2026,2,8));
        assertThat(resp.getItems().get(0).getPosts()).isEqualTo(1);
        assertThat(resp.getItems().get(0).getComments()).isEqualTo(1);

        assertThat(resp.getItems().get(1).getDate()).isEqualTo(LocalDate.of(2026,2,9));
        assertThat(resp.getItems().get(1).getLikes()).isEqualTo(1);

        assertThat(resp.getItems().get(2).getDate()).isEqualTo(LocalDate.of(2026,2,10));
        assertThat(resp.getItems().get(2).getPosts()).isEqualTo(1);
    }

    @Test
    void getActivity_ShouldHandleLeapDayBoundary() {
        // ensure all tables used by the service exist (knowledge, comment, like)
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS knowledge (id BIGINT PRIMARY KEY, created_at TIMESTAMP, is_deleted BOOLEAN);");
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS comment (id BIGINT PRIMARY KEY, created_at TIMESTAMP, is_deleted BOOLEAN);");
        jdbcTemplate.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS \"like\" (id BIGINT PRIMARY KEY, created_at TIMESTAMP);");

        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE knowledge;");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE comment;");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE \"like\";");

        jdbcTemplate.getJdbcTemplate().update("INSERT INTO knowledge (id, created_at, is_deleted) VALUES (10, ?, false)", new Object[]{Timestamp.valueOf(LocalDateTime.of(2020,2,29,23,59))});

        LocalDate from = LocalDate.of(2020,2,28);
        LocalDate to = LocalDate.of(2020,3,1);

        ActivityResponse resp = dashboardService.getActivity(from, to, "week");
        List<?> items = resp.getItems();
        assertThat(items).hasSize(3);
        assertThat(resp.getItems().get(1).getDate()).isEqualTo(LocalDate.of(2020,2,29));
        assertThat(resp.getItems().get(1).getPosts()).isEqualTo(1);
    }
}
