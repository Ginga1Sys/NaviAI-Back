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
import com.ginga.naviai.dashboard.dto.ActivityResponse;
import com.ginga.naviai.dashboard.dto.ActivityDayItem;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
     * 【正常系】`/api/v1/dashboard/activity` が期待フォーマットで応答することを確認する。
     */
    @Test
    @WithMockUser
    void getActivity_ShouldReturnDailyActivity() throws Exception {
        ActivityResponse activity = ActivityResponse.builder()
            .range("week")
            .from(LocalDate.of(2026,2,8))
            .to(LocalDate.of(2026,2,10))
            .items(List.of(
                new ActivityDayItem(LocalDate.of(2026,2,8), 3,5,10),
                new ActivityDayItem(LocalDate.of(2026,2,9), 1,2,4)
            ))
            .build();

        when(dashboardService.getActivity(any(LocalDate.class), any(LocalDate.class), anyString())).thenReturn(activity);

        mockMvc.perform(get("/api/v1/dashboard/activity?from=2026-02-08&to=2026-02-10&range=week")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.range").value("week"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].posts").value(3));
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

    /**
     * 【異常系】未認証ユーザーが `/activity` にアクセスした場合、401 が返却されることを確認する。
     */
    @Test
    void getActivity_ShouldReturnUnauthorized_WhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/activity?from=2026-02-08&to=2026-02-10&range=week")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    /**
     * 【異常系】`from` または `to` が欠落している場合は 400 (Bad Request) を返すこと。
     */
    @Test
    @WithMockUser
    void getActivity_ShouldReturnBadRequest_WhenMissingParams() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/activity?from=2026-02-08")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/dashboard/activity?to=2026-02-10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * 【異常系】日付形式が不正な場合は 400 (Bad Request) を返すこと。
     */
    @Test
    @WithMockUser
    void getActivity_ShouldReturnBadRequest_WhenInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/activity?from=2026-02-08&to=not-a-date&range=week")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
