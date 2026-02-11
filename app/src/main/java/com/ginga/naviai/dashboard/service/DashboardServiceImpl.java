package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final KnowledgeRepository knowledgeRepository;

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

        return DashboardSummaryResponse.builder()
            .totalPosts(totalPosts)
            .weeklyPosts(weeklyPosts)
            .pendingApprovals(pendingApprovals)
            .topTags(topTags)
            .recentArticles(recentArticles)
            .recommendedArticles(recommendedArticles)
            .build();
    }
}
