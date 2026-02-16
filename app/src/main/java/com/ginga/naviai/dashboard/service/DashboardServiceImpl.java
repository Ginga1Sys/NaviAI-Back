package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.ActivityDayItem;
import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final KnowledgeRepository knowledgeRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalPosts = knowledgeRepository.countByDeletedFalse();
        
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long weeklyPosts = knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(weekAgo);
        
        long pendingApprovals = knowledgeRepository.countByStatusAndDeletedFalse("pending");
        
        List<Object[]> topTagsData = knowledgeRepository.findTopTags(5);
        List<DashboardSummaryResponse.TagSummary> topTags = topTagsData.stream()
            .map(row -> new DashboardSummaryResponse.TagSummary((String) row[0], ((Number) row[1]).longValue()))
            .collect(Collectors.toList());

        // 新着記事 (Top 5)
        List<Knowledge> recentEntities = knowledgeRepository.findRecentArticles(PageRequest.of(0, 5));
        List<DashboardSummaryResponse.ArticleSummary> recentArticles = recentEntities.stream()
            .map(k -> DashboardSummaryResponse.ArticleSummary.builder()
                .id(k.getId())
                .title(k.getTitle())
                .authorDisplayName(k.getAuthor() != null ? k.getAuthor().getDisplayName() : "不明")
                .publishedAt(k.getPublishedAt())
                .likeCount(0) // 簡略化のため新着一覧では0
                .build())
            .collect(Collectors.toList());

        // おすすめ記事 (Top 5 by Likes)
        List<Object[]> recommendedData = knowledgeRepository.findTopRecommendedArticles(5);
        List<DashboardSummaryResponse.ArticleSummary> recommendedArticles = recommendedData.stream()
            .map(row -> DashboardSummaryResponse.ArticleSummary.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .authorDisplayName((String) row[2])
                .publishedAt(row[3] != null ? ((Timestamp) row[3]).toInstant() : null)
                .likeCount(((Number) row[4]).longValue())
                .build())
            .collect(Collectors.toList());

        // 週次アクティビティ（直近4週）
        int weeks = 4;
        Instant now = Instant.now();
        List<DashboardSummaryResponse.WeeklyActivity> weeklyActivity = new ArrayList<>();
        for (int i = weeks - 1; i >= 0; i--) {
            Instant start = now.minus((long)(i + 1) * 7, ChronoUnit.DAYS);
            Instant end = now.minus((long)i * 7, ChronoUnit.DAYS);
            long count = knowledgeRepository.countByCreatedAtBetweenAndDeletedFalse(start, end);
            weeklyActivity.add(new DashboardSummaryResponse.WeeklyActivity(start, count));
        }

        return DashboardSummaryResponse.builder()
            .totalPosts(totalPosts)
            .weeklyPosts(weeklyPosts)
            .pendingApprovals(pendingApprovals)
            .weeklyActivity(weeklyActivity)
            .topTags(topTags)
            .recentArticles(recentArticles)
            .recommendedArticles(recommendedArticles)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getActivity(LocalDate from, LocalDate to, String range) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to must be provided");
        }
        // ensure from <= to
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", java.sql.Date.valueOf(from));
        params.addValue("to", java.sql.Date.valueOf(to));

        String postsSql = "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
                "FROM knowledge " +
                "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to AND is_deleted = false " +
                "GROUP BY dt ORDER BY dt";

        String commentsSql = "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
                "FROM comment " +
                "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to AND is_deleted = false " +
                "GROUP BY dt ORDER BY dt";

        String likesSql = "SELECT CAST(created_at AS DATE) as dt, COUNT(*) as cnt " +
                "FROM \"like\" " +
                "WHERE CAST(created_at AS DATE) BETWEEN :from AND :to " +
                "GROUP BY dt ORDER BY dt";

        Map<LocalDate, Integer> postsMap = jdbcTemplate.query(postsSql, params, rs -> {
            Map<LocalDate, Integer> m = new LinkedHashMap<>();
            while (rs.next()) {
                java.sql.Date d = rs.getDate("dt");
                int c = rs.getInt("cnt");
                m.put(d.toLocalDate(), c);
            }
            return m;
        });

        Map<LocalDate, Integer> commentsMap = jdbcTemplate.query(commentsSql, params, rs -> {
            Map<LocalDate, Integer> m = new LinkedHashMap<>();
            while (rs.next()) {
                java.sql.Date d = rs.getDate("dt");
                int c = rs.getInt("cnt");
                m.put(d.toLocalDate(), c);
            }
            return m;
        });

        Map<LocalDate, Integer> likesMap = jdbcTemplate.query(likesSql, params, rs -> {
            Map<LocalDate, Integer> m = new LinkedHashMap<>();
            while (rs.next()) {
                java.sql.Date d = rs.getDate("dt");
                int c = rs.getInt("cnt");
                m.put(d.toLocalDate(), c);
            }
            return m;
        });

        List<ActivityDayItem> items = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            int p = postsMap.getOrDefault(cur, 0);
            int c = commentsMap.getOrDefault(cur, 0);
            int l = likesMap.getOrDefault(cur, 0);
            items.add(ActivityDayItem.builder()
                    .date(cur)
                    .posts(p)
                    .comments(c)
                    .likes(l)
                    .build());
            cur = cur.plusDays(1);
        }

        return ActivityResponse.builder()
                .range(range != null ? range : "week")
                .from(from)
                .to(to)
                .items(items)
                .build();
    }
}
