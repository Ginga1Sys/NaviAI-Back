package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import com.ginga.naviai.knowledge.dto.PublicKnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.PublicKnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.PublicRecommendedKnowledgeResponse;
import com.ginga.naviai.knowledge.service.PublicKnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicKnowledgeController.class)
class PublicKnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicKnowledgeService publicKnowledgeService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void getPublicKnowledge_returns200AndItems() throws Exception {
        PublicKnowledgePageResponse response = PublicKnowledgePageResponse.builder()
                .page(0)
                .size(10)
                .totalElements(1)
                .items(List.of(
                        PublicKnowledgeItemDto.builder()
                                .id(2001L)
                                .title("公開記事A")
                                .excerpt("概要")
                                .thumbnail(null)
                                .authorDisplayName("田中 太郎")
                                .publishedAt(Instant.parse("2026-03-20T00:00:00Z"))
                                .tags(List.of("ChatGPT", "業務効率化"))
                                .build()
                ))
                .build();

        when(publicKnowledgeService.getPublicKnowledge(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/knowledge")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(2001))
                .andExpect(jsonPath("$.items[0].title").value("公開記事A"));
    }

    @Test
    void getPublicKnowledge_withNegativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/public/knowledge")
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecommendedKnowledge_returns200AndItems() throws Exception {
        PublicRecommendedKnowledgeResponse response = PublicRecommendedKnowledgeResponse.builder()
                .items(List.of(
                        PublicRecommendedKnowledgeItemDto.builder()
                                .id(1001L)
                                .title("公開テスト記事 A")
                                .likeCount(15)
                                .build()
                ))
                .build();

        when(publicKnowledgeService.getRecommendedKnowledge(eq(1))).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/knowledge/recommended")
                        .param("limit", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1001))
                .andExpect(jsonPath("$.items[0].title").value("公開テスト記事 A"))
                .andExpect(jsonPath("$.items[0].likeCount").value(15));
    }
}
