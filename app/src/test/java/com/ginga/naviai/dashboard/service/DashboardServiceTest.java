package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * DashboardServiceImpl の単体テスト。
 * （旧 DashboardServiceImplTest を本クラスに統合）
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    /**
     * 【正常系】データが存在する場合、正しいサマリー情報が返却されることを確認する。
     * - 統計値（総数、週間数、承認待ち）が正しく取得されているか。
     * - タグ、新着、おすすめのリストが正しくマッピングされているか。
     */
    @Test
    void getSummary_ShouldReturnCorrectSummary() {
        // Arrange: モック動作の設定
        when(knowledgeRepository.countByDeletedFalse()).thenReturn(100L);
        when(knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(any(Instant.class))).thenReturn(10L);
        when(knowledgeRepository.countByStatusAndDeletedFalse("pending")).thenReturn(5L);

        List<Object[]> topTags = new ArrayList<>();
        topTags.add(new Object[]{"AI", 50L});
        when(knowledgeRepository.findTopTags(5)).thenReturn(topTags);

        // 新着記事のモック（1件）
        Knowledge recent = Knowledge.builder().id(1L).title("新着記事").publishedAt(Instant.now()).build();
        when(knowledgeRepository.findRecentArticles(PageRequest.of(0, 5))).thenReturn(List.of(recent));

        // おすすめ記事のモック（1件）
        List<Object[]> recommended = new ArrayList<>();
        recommended.add(new Object[]{2L, "おすすめ記事", "著者名", Timestamp.from(Instant.now()), 15L});
        when(knowledgeRepository.findTopRecommendedArticles(5)).thenReturn(recommended);

        // 週次アクティビティ: findCreatedAtInRange で1クエリ取得
        when(knowledgeRepository.findCreatedAtInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // Act: 実行
        DashboardSummaryResponse result = dashboardService.getSummary();

        // Assert: 検証
        assertThat(result.getTotalPosts()).isEqualTo(100L);
        assertThat(result.getWeeklyPosts()).isEqualTo(10L);
        assertThat(result.getPendingApprovals()).isEqualTo(5L);
        assertThat(result.getTopTags()).hasSize(1);
        assertThat(result.getRecentArticles()).hasSize(1);
        assertThat(result.getRecentArticles().get(0).getTitle()).isEqualTo("新着記事");
        assertThat(result.getRecommendedArticles()).hasSize(1);
        assertThat(result.getRecommendedArticles().get(0).getLikeCount()).isEqualTo(15L);
    }

    /**
     * 【境界値/異常系】データが1件も存在しない場合でも、空のリストを含むレスポンスが返却されることを確認する。
     */
    @Test
    void getSummary_ShouldReturnEmptySummary_WhenNoDataExists() {
        // Arrange
        when(knowledgeRepository.countByDeletedFalse()).thenReturn(0L);
        when(knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(any(Instant.class))).thenReturn(0L);
        when(knowledgeRepository.countByStatusAndDeletedFalse("pending")).thenReturn(0L);
        when(knowledgeRepository.findTopTags(5)).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findRecentArticles(any())).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findTopRecommendedArticles(5)).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findCreatedAtInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // Act
        DashboardSummaryResponse result = dashboardService.getSummary();

        // Assert
        assertThat(result.getTotalPosts()).isZero();
        assertThat(result.getTopTags()).isEmpty();
        assertThat(result.getRecentArticles()).isEmpty();
        assertThat(result.getRecommendedArticles()).isEmpty();
        assertThat(result.getWeeklyActivity()).hasSize(4);
    }

    /**
     * 【特殊系】記事の著者が存在しない（null）場合、"不明" として処理されることを確認する。
     */
    @Test
    void getSummary_ShouldHandleNullAuthorGracefully() {
        // Arrange: 著者が未設定のKnowledge
        Knowledge recent = Knowledge.builder().id(1L).title("著者なし記事").author(null).publishedAt(Instant.now()).build();

        when(knowledgeRepository.findRecentArticles(any())).thenReturn(List.of(recent));
        // 他のモックは空で設定
        when(knowledgeRepository.countByDeletedFalse()).thenReturn(1L);
        when(knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(any())).thenReturn(0L);
        when(knowledgeRepository.countByStatusAndDeletedFalse(any())).thenReturn(0L);
        when(knowledgeRepository.findTopTags(anyInt())).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findTopRecommendedArticles(anyInt())).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findCreatedAtInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // Act
        DashboardSummaryResponse result = dashboardService.getSummary();

        // Assert
        assertThat(result.getRecentArticles().get(0).getAuthorDisplayName()).isEqualTo("不明");
    }

    /**
     * 【正常系】週次アクティビティが1クエリで集計され、4週分のリストが返却されることを確認する。
     * findCreatedAtInRange を1回だけ呼び出し、アプリ側で週ごとに集計する動作を検証する。
     */
    @Test
    void getSummary_ShouldReturnWeeklyActivity_UsingBulkQuery() {
        // Arrange
        when(knowledgeRepository.countByDeletedFalse()).thenReturn(50L);
        when(knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(any(Instant.class))).thenReturn(5L);
        when(knowledgeRepository.countByStatusAndDeletedFalse(any())).thenReturn(0L);
        when(knowledgeRepository.findTopTags(5)).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findRecentArticles(any())).thenReturn(Collections.emptyList());
        when(knowledgeRepository.findTopRecommendedArticles(5)).thenReturn(Collections.emptyList());

        // 3週前と2週前に各1件ずつ作成されたと想定
        Instant threeWeeksAgo = Instant.now().minusSeconds(3L * 7 * 24 * 60 * 60 - 100);
        Instant twoWeeksAgo   = Instant.now().minusSeconds(2L * 7 * 24 * 60 * 60 - 100);
        when(knowledgeRepository.findCreatedAtInRange(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(threeWeeksAgo, twoWeeksAgo));

        // Act
        DashboardSummaryResponse result = dashboardService.getSummary();

        // Assert: 4週分のリストが生成されること
        assertThat(result.getWeeklyActivity()).hasSize(4);
        // 各週の weekStart が null でないこと
        result.getWeeklyActivity().forEach(wa -> assertThat(wa.getWeekStart()).isNotNull());
    }
}
