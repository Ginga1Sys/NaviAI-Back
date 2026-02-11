package com.ginga.naviai.dashboard.controller;

import com.ginga.naviai.dashboard.dto.DashboardSummaryResponse;
import com.ginga.naviai.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ginga.naviai.auth.service.TokenBlacklistService;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 【正常系】ログイン済みユーザーがダッシュボード情報を取得できることを確認する。
     * - ステータスコード 200 (OK)
     * - レスポンスJSONに期待されるフィールドが含まれているか。
     */
    @Test
    @WithMockUser
    void getSummary_ShouldReturnOk_WithMockUser() throws Exception {
        // Arrange
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
            .totalPosts(100)
            .weeklyPosts(10)
            .pendingApprovals(5)
            .topTags(Collections.emptyList())
            .recentArticles(Collections.emptyList())
            .recommendedArticles(Collections.emptyList())
            .build();
        
        when(dashboardService.getSummary()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPosts").value(100))
            .andExpect(jsonPath("$.weeklyPosts").value(10))
            .andExpect(jsonPath("$.pendingApprovals").value(5))
            .andExpect(jsonPath("$.recentArticles").isArray())
            .andExpect(jsonPath("$.recommendedArticles").isArray());
    }

    /**
     * 【異常系】未ログインのユーザーがアクセスした場合、401 (Unauthorized) が返却されることを確認する。
     * ※ Spring Security の設定により制御される。
     */
    @Test
    void getSummary_ShouldReturnUnauthorized_WhenNotLoggedIn() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
