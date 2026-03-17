package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.ActivityResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
class DashboardActivityServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private com.ginga.naviai.knowledge.repository.KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    /**
     * サービスの `getActivity` が日付レンジの各日を補完して返すことを検証する。
     */
    @Test
    void getActivity_ShouldReturnDailyCounts_WithZeroFill() {
        LocalDate from = LocalDate.of(2026, 2, 8);
        LocalDate to = LocalDate.of(2026, 2, 10);

        Map<LocalDate, Integer> posts = new LinkedHashMap<>();
        posts.put(LocalDate.of(2026, 2, 8), 3);
        posts.put(LocalDate.of(2026, 2, 10), 1);

        Map<LocalDate, Integer> comments = new LinkedHashMap<>();
        comments.put(LocalDate.of(2026, 2, 8), 5);

        Map<LocalDate, Integer> likes = new LinkedHashMap<>();
        likes.put(LocalDate.of(2026, 2, 9), 4);

        // jdbcTemplate.query(...) の呼び出しに対して SQL内容で振り分けて戻り値を返す
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                if (sql.contains("FROM knowledge")) return posts;
                if (sql.contains("FROM comment")) return comments;
                return likes;
            });

        ActivityResponse resp = dashboardService.getActivity(from, to, "week");

        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(3);
        // 2026-02-08
        assertThat(resp.getItems().get(0).getDate()).isEqualTo(LocalDate.of(2026,2,8));
        assertThat(resp.getItems().get(0).getPosts()).isEqualTo(3);
        assertThat(resp.getItems().get(0).getComments()).isEqualTo(5);
        assertThat(resp.getItems().get(0).getLikes()).isEqualTo(0);

        // 2026-02-09 (missing posts/comments -> zero)
        assertThat(resp.getItems().get(1).getDate()).isEqualTo(LocalDate.of(2026,2,9));
        assertThat(resp.getItems().get(1).getPosts()).isEqualTo(0);
        assertThat(resp.getItems().get(1).getComments()).isEqualTo(0);
        assertThat(resp.getItems().get(1).getLikes()).isEqualTo(4);

        // 2026-02-10
        assertThat(resp.getItems().get(2).getDate()).isEqualTo(LocalDate.of(2026,2,10));
        assertThat(resp.getItems().get(2).getPosts()).isEqualTo(1);
        assertThat(resp.getItems().get(2).getComments()).isEqualTo(0);
        assertThat(resp.getItems().get(2).getLikes()).isEqualTo(0);
    }

    @Test
    void getActivity_ShouldReturnZeros_WhenNoData() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 3);

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
            .thenReturn(new LinkedHashMap<LocalDate, Integer>());

        ActivityResponse resp = dashboardService.getActivity(from, to, "week");

        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(3);
        resp.getItems().forEach(i -> {
            assertThat(i.getPosts()).isEqualTo(0);
            assertThat(i.getComments()).isEqualTo(0);
            assertThat(i.getLikes()).isEqualTo(0);
        });
    }

    @Test
    void getActivity_ShouldThrow_WhenParamsNull() {
        LocalDate to = LocalDate.of(2026, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> dashboardService.getActivity(null, to, "week"));
        LocalDate from = LocalDate.of(2026, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> dashboardService.getActivity(from, null, "week"));
    }

    @Test
    void getActivity_ShouldThrow_WhenFromAfterTo() {
        LocalDate from = LocalDate.of(2026, 2, 4);
        LocalDate to = LocalDate.of(2026, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> dashboardService.getActivity(from, to, "week"));
    }

    @Test
    void getActivity_ShouldPropagate_WhenJdbcThrows() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 3);

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
            .thenThrow(new RuntimeException("db error"));

        assertThrows(RuntimeException.class, () -> dashboardService.getActivity(from, to, "week"));
    }
}
