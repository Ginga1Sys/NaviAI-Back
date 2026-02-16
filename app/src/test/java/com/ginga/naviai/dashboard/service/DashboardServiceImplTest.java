package com.ginga.naviai.dashboard.service;

import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.Tag;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceImplTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getSummary_returnsAggregatedData() {
        when(knowledgeRepository.countByDeletedFalse()).thenReturn(100L);
        when(knowledgeRepository.countByCreatedAtAfterAndDeletedFalse(any(Instant.class))).thenReturn(5L);
        when(knowledgeRepository.countByStatusAndDeletedFalse("pending")).thenReturn(3L);

        // top tags
        Object[] t1 = new Object[]{"java", 10L};
        Object[] t2 = new Object[]{"spring", 6L};
        java.util.List<Object[]> topTags = new java.util.ArrayList<>();
        topTags.add(t1);
        topTags.add(t2);
        when(knowledgeRepository.findTopTags(5)).thenReturn(topTags);

        // recent articles (return two Knowledge entities)
        Knowledge k1 = Knowledge.builder().id(11L).title("A").publishedAt(Instant.now()).build();
        Knowledge k2 = Knowledge.builder().id(12L).title("B").publishedAt(Instant.now()).build();
        when(knowledgeRepository.findRecentArticles(PageRequest.of(0,5))).thenReturn(List.of(k1, k2));

        // recommended articles (native query rows)
        Object[] r1 = new Object[]{1L, "Title1", "Author1", Timestamp.from(Instant.now()), 42L};
        java.util.List<Object[]> recommended = new java.util.ArrayList<>();
        recommended.add(r1);
        when(knowledgeRepository.findTopRecommendedArticles(5)).thenReturn(recommended);

        // weekly aggregation: return constant for any week bucket
        when(knowledgeRepository.countByCreatedAtBetweenAndDeletedFalse(any(Instant.class), any(Instant.class))).thenReturn(7L);

        DashboardSummaryResponse resp = dashboardService.getSummary();

        assertNotNull(resp);
        assertEquals(100L, resp.getTotalPosts());
        assertEquals(5L, resp.getWeeklyPosts());
        assertEquals(3L, resp.getPendingApprovals());

        assertNotNull(resp.getTopTags());
        assertEquals(2, resp.getTopTags().size());
        assertEquals("java", resp.getTopTags().get(0).getTag());
        assertEquals(10L, resp.getTopTags().get(0).getCount());

        assertNotNull(resp.getWeeklyActivity());
        assertEquals(4, resp.getWeeklyActivity().size());
        for (DashboardSummaryResponse.WeeklyActivity wa : resp.getWeeklyActivity()) {
            assertEquals(7L, wa.getCount());
            assertNotNull(wa.getWeekStart());
        }
    }
}
