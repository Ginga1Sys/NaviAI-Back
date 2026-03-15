package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeResponse;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicKnowledgeServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private PublicKnowledgeServiceImpl publicKnowledgeService;

    @Test
    void getRecommendedKnowledge_returnsMappedItems() {
        when(knowledgeRepository.findTopRecommendedArticlesAll(eq(1)))
                .thenReturn(List.<Object[]>of(new Object[]{101L, "おすすめ記事A", 7L}));

        PublicRecommendedKnowledgeResponse response = publicKnowledgeService.getRecommendedKnowledge(1);

        assertEquals(1, response.getItems().size());
        assertEquals(101L, response.getItems().get(0).getId());
        assertEquals("おすすめ記事A", response.getItems().get(0).getTitle());
        assertEquals(7L, response.getItems().get(0).getLikeCount());
        verify(knowledgeRepository).findTopRecommendedArticlesAll(eq(1));
    }

    @Test
    void getRecommendedKnowledge_capsLimitToRange() {
        when(knowledgeRepository.findTopRecommendedArticlesAll(eq(10)))
            .thenReturn(List.<Object[]>of());

        publicKnowledgeService.getRecommendedKnowledge(99);

        verify(knowledgeRepository).findTopRecommendedArticlesAll(eq(10));
    }
}
