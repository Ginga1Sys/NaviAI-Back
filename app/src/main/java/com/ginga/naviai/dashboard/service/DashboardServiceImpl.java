package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.ActivityDayItem;
import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.dashboard.repository.ActivityQueryConstants;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import com.ginga.naviai.util.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
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
        // likeCount: 新着一覧の取得コストを抑えるため likeCount は 0 固定とする。
        // 詳細は API 仕様書「GET /api/v1/dashboard」の recentArticles 項を参照。
        List<Knowledge> recentEntities = knowledgeRepository.findRecentArticles(PageRequest.of(0, 5));
        List<DashboardSummaryResponse.ArticleSummary> recentArticles = recentEntities.stream()
            .map(k -> DashboardSummaryResponse.ArticleSummary.builder()
                .id(k.getId())
                .title(k.getTitle())
                .authorDisplayName(k.getAuthor() != null ? k.getAuthor().getDisplayName() : "不明")
                .publishedAt(k.getPublishedAt())
                .likeCount(0) // 新着一覧ではクエリコスト削減のため 0 固定（仕様）
                .build())
            .collect(Collectors.toList());

        // おすすめ記事 (Top 5 by Likes)
        List<Object[]> recommendedData = knowledgeRepository.findTopRecommendedArticles(5);
        List<DashboardSummaryResponse.ArticleSummary> recommendedArticles = recommendedData.stream()
            .map(row -> DashboardSummaryResponse.ArticleSummary.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .authorDisplayName((String) row[2])
                .publishedAt(DateTimeUtils.toInstant(row[3]))
                .likeCount(((Number) row[4]).longValue())
                .build())
            .collect(Collectors.toList());

        // 週次アクティビティ（直近4週）
        // N+1 問題を回避するため、4週分の全作成日時を1クエリで取得してアプリ側で集計する。
        int weeks = 4;
        Instant now = Instant.now();
        Instant globalStart = now.minus((long) weeks * 7, ChronoUnit.DAYS);
        List<Instant> createdAts = knowledgeRepository.findCreatedAtInRange(globalStart, now);

        List<DashboardSummaryResponse.WeeklyActivity> weeklyActivity = new ArrayList<>();
        for (int i = weeks - 1; i >= 0; i--) {
            Instant start = now.minus((long)(i + 1) * 7, ChronoUnit.DAYS);
            Instant end   = now.minus((long) i      * 7, ChronoUnit.DAYS);
            long count = createdAts.stream()
                .filter(t -> !t.isBefore(start) && t.isBefore(end))
                .count();
            weeklyActivity.add(DashboardSummaryResponse.WeeklyActivity.builder()
                .weekStart(start)
                .count(count)
                .build());
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
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", java.sql.Date.valueOf(from));
        params.addValue("to", java.sql.Date.valueOf(to));

        Map<LocalDate, Integer> postsMap    = queryDateCountMap(ActivityQueryConstants.POSTS_BY_DAY, params);
        Map<LocalDate, Integer> commentsMap = queryDateCountMap(ActivityQueryConstants.COMMENTS_BY_DAY, params);
        Map<LocalDate, Integer> likesMap    = queryDateCountMap(ActivityQueryConstants.LIKES_BY_DAY, params);

        List<ActivityDayItem> items = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            items.add(ActivityDayItem.builder()
                    .date(cur)
                    .posts(postsMap.getOrDefault(cur, 0))
                    .comments(commentsMap.getOrDefault(cur, 0))
                    .likes(likesMap.getOrDefault(cur, 0))
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

    /** 日付→件数のマップを返す共通ヘルパー */
    private Map<LocalDate, Integer> queryDateCountMap(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.query(sql, params, rs -> {
            Map<LocalDate, Integer> m = new LinkedHashMap<>();
            while (rs.next()) {
                java.sql.Date d = rs.getDate("dt");
                int c = rs.getInt("cnt");
                m.put(d.toLocalDate(), c);
            }
            return m;
        });
    }

}
