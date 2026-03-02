package com.ginga.naviai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ダッシュボードのサマリー情報を返すDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    /** 総投稿数 */
    private long totalPosts;

    /** 今週の投稿数 */
    private long weeklyPosts;

    /** 承認待ち件数 */
    private long pendingApprovals;

    /** 週次アクティビティ（週開始日時・件数） */
    private java.util.List<WeeklyActivity> weeklyActivity;

    /** 人気タグ上位 */
    private List<TagSummary> topTags;

    /** 新着記事 */
    private List<ArticleSummary> recentArticles;

    /** おすすめ記事 */
    private List<ArticleSummary> recommendedArticles;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TagSummary {
        private String tag;
        private long count;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ArticleSummary {
        private Long id;
        private String title;
        private String authorDisplayName;
        private java.time.Instant publishedAt;
        private long likeCount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeeklyActivity {
        private java.time.Instant weekStart;
        private long count;
    }
}
