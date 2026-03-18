package com.ginga.naviai.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminConflictException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.service.AdminKnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminKnowledgeController のユニットテスト
 * サービス層をモックし、各エンドポイントのHTTPレスポンスを検証する
 */
@WebMvcTest(AdminKnowledgeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminKnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminKnowledgeService knowledgeService;

    // SecurityConfig 関連のモック
    @MockBean
    private com.ginga.naviai.auth.service.TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ObjectMapper mapper;

    // ========== GET /api/v1/admin/knowledge (一覧) ==========

    @Test
    void list_defaultParams_returns200() throws Exception {
        // デフォルトパラメータで一覧取得が200を返すことを検証する
        PagedResponse<KnowledgeSummaryResponse> res = new PagedResponse<>();
        res.setData(List.of());
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(20);
        meta.setTotal(0);
        res.setMeta(meta);

        when(knowledgeService.list(eq("pending"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq(20), isNull())).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void list_withQueryParams_returns200() throws Exception {
        // クエリパラメータ付きで一覧取得が200を返すことを検証する
        KnowledgeSummaryResponse item = new KnowledgeSummaryResponse();
        item.setId("k1");
        item.setTitle("Test Knowledge");
        item.setStatus("pending");

        PagedResponse<KnowledgeSummaryResponse> res = new PagedResponse<>();
        res.setData(List.of(item));
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(10);
        meta.setTotal(1);
        res.setMeta(meta);

        when(knowledgeService.list(eq("published"), eq("search"), eq("1"), eq("java"),
                isNull(), isNull(), eq(1), eq(10), eq("-submitted_at"))).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/knowledge")
                        .param("status", "published")
                        .param("q", "search")
                        .param("author_id", "1")
                        .param("tag", "java")
                        .param("per_page", "10")
                        .param("sort", "-submitted_at"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("k1"))
                .andExpect(jsonPath("$.data[0].title").value("Test Knowledge"));
    }

    // ========== GET /api/v1/admin/knowledge/{id} (詳細) ==========

    @Test
    void getDetail_found_returns200() throws Exception {
        // 存在するナレッジの詳細取得が200を返すことを検証する
        KnowledgeDetailResponse detail = new KnowledgeDetailResponse();
        detail.setId("k1");
        detail.setTitle("Title");
        detail.setBody("Body text");
        detail.setStatus("pending");
        detail.setTags(List.of("tag1"));

        when(knowledgeService.getDetail("k1")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/knowledge/k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("k1"))
                .andExpect(jsonPath("$.data.title").value("Title"))
                .andExpect(jsonPath("$.data.tags[0]").value("tag1"));
    }

    @Test
    void getDetail_notFound_returns404() throws Exception {
        // 存在しないナレッジの詳細取得が404を返すことを検証する
        when(knowledgeService.getDetail("missing")).thenThrow(new AdminNotFoundException("Knowledge not found"));

        mockMvc.perform(get("/api/v1/admin/knowledge/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // ========== POST /api/v1/admin/knowledge/{id}/approve ==========

    @Test
    void approve_success_returns200() throws Exception {
        // 承認成功が200とステータス変更を返すことを検証する
        SimpleStatusResponse ssr = new SimpleStatusResponse();
        ssr.setId("k1");
        ssr.setStatus("published");
        ssr.setPublishedAt(Instant.now().toString());

        when(knowledgeService.approve(eq("k1"), isNull())).thenReturn(ssr);

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("k1"))
                .andExpect(jsonPath("$.data.status").value("published"));
    }

    @Test
    void approve_withNote_returns200() throws Exception {
        // ノート付き承認が200を返すことを検証する
        SimpleStatusResponse ssr = new SimpleStatusResponse();
        ssr.setId("k1");
        ssr.setStatus("published");

        when(knowledgeService.approve(eq("k1"), eq("Good article"))).thenReturn(ssr);

        ApprovalRequest req = new ApprovalRequest();
        req.setNote("Good article");

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"));
    }

    @Test
    void approve_notFound_returns404() throws Exception {
        // 存在しないナレッジの承認が404を返すことを検証する
        when(knowledgeService.approve(eq("missing"), isNull()))
                .thenThrow(new AdminNotFoundException("Knowledge not found"));

        mockMvc.perform(post("/api/v1/admin/knowledge/missing/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void approve_alreadyPublished_returns409() throws Exception {
        // 既に公開済みのナレッジの承認が409を返すことを検証する
        when(knowledgeService.approve(eq("k1"), isNull()))
                .thenThrow(new AdminConflictException("Only pending knowledge can be approved"));

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    // ========== POST /api/v1/admin/knowledge/{id}/reject ==========

    @Test
    void reject_success_returns200() throws Exception {
        // 却下成功が200を返すことを検証する
        SimpleStatusResponse ssr = new SimpleStatusResponse();
        ssr.setId("k1");
        ssr.setStatus("declined");

        when(knowledgeService.reject(eq("k1"), eq("Low quality"))).thenReturn(ssr);

        RejectRequest req = new RejectRequest();
        req.setReason("Low quality");

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("declined"));
    }

    @Test
    void reject_missingReason_returns400() throws Exception {
        // reason 未指定の却下が400を返すことを検証する
        RejectRequest req = new RejectRequest();
        // reason is null → @NotBlank validation fails

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_conflict_returns409() throws Exception {
        // pending でないナレッジの却下が409を返すことを検証する
        when(knowledgeService.reject(eq("k1"), anyString()))
                .thenThrow(new AdminConflictException("Only pending knowledge can be rejected"));

        RejectRequest req = new RejectRequest();
        req.setReason("Some reason");

        mockMvc.perform(post("/api/v1/admin/knowledge/k1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ========== POST /api/v1/admin/knowledge/bulk-action ==========

    @Test
    void bulkAction_approve_returns200() throws Exception {
        // 一括承認が200とサマリーを返すことを検証する
        BulkActionResultResponse.BulkItemResult item1 = new BulkActionResultResponse.BulkItemResult();
        item1.setId("k1");
        item1.setOk(true);
        BulkActionResultResponse.Summary summary = new BulkActionResultResponse.Summary();
        summary.setOk(1);
        summary.setFailed(0);
        BulkActionResultResponse bulkRes = new BulkActionResultResponse();
        bulkRes.setAction("approve");
        bulkRes.setResults(List.of(item1));
        bulkRes.setSummary(summary);

        when(knowledgeService.bulkAction(any(BulkActionRequest.class))).thenReturn(bulkRes);

        BulkActionRequest req = new BulkActionRequest();
        req.setAction("approve");
        req.setIds(List.of("k1"));

        mockMvc.perform(post("/api/v1/admin/knowledge/bulk-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.action").value("approve"))
                .andExpect(jsonPath("$.data.summary.ok").value(1))
                .andExpect(jsonPath("$.data.summary.failed").value(0));
    }

    @Test
    void bulkAction_missingActionField_returns400() throws Exception {
        // action フィールド未指定が400を返すことを検証する
        String body = "{\"ids\":[\"k1\"]}";

        mockMvc.perform(post("/api/v1/admin/knowledge/bulk-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkAction_emptyIds_returns400() throws Exception {
        // ids が空の場合400を返すことを検証する
        String body = "{\"action\":\"approve\",\"ids\":[]}";

        mockMvc.perform(post("/api/v1/admin/knowledge/bulk-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/admin/knowledge/{id}/moderation ==========

    @Test
    void getModeration_found_returns200() throws Exception {
        // モデレーション情報取得が200を返すことを検証する
        ModerationResponse mr = new ModerationResponse();
        mr.setKnowledgeId("k1");
        mr.setInternalNote("Check sources");

        when(knowledgeService.getModeration("k1")).thenReturn(mr);

        mockMvc.perform(get("/api/v1/admin/knowledge/k1/moderation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.knowledge_id").value("k1"))
                .andExpect(jsonPath("$.data.internal_note").value("Check sources"));
    }

    @Test
    void getModeration_notFound_returns404() throws Exception {
        // モデレーション情報が存在しない場合404を返すことを検証する
        when(knowledgeService.getModeration("missing"))
                .thenThrow(new AdminNotFoundException("Moderation not found"));

        mockMvc.perform(get("/api/v1/admin/knowledge/missing/moderation"))
                .andExpect(status().isNotFound());
    }

    // ========== PUT /api/v1/admin/knowledge/{id}/moderation ==========

    @Test
    void putModeration_success_returns200() throws Exception {
        // モデレーション更新が200を返すことを検証する
        ModerationResponse mr = new ModerationResponse();
        mr.setKnowledgeId("k1");
        mr.setInternalNote("Updated note");

        when(knowledgeService.updateModeration(eq("k1"), any(ModerationRequest.class))).thenReturn(mr);

        ModerationRequest req = new ModerationRequest();
        req.setInternalNote("Updated note");

        mockMvc.perform(put("/api/v1/admin/knowledge/k1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internal_note").value("Updated note"));
    }

    // ========== GET /api/v1/admin/knowledge/stats ==========

    @Test
    void stats_returns200() throws Exception {
        // 統計情報取得が200を返すことを検証する
        StatsResponse stats = new StatsResponse();
        stats.setPending(5);
        stats.setPublished(10);
        stats.setDeclined(2);

        when(knowledgeService.stats(isNull(), isNull())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/knowledge/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(5))
                .andExpect(jsonPath("$.data.published").value(10))
                .andExpect(jsonPath("$.data.declined").value(2));
    }

    @Test
    void stats_withDateRange_returns200() throws Exception {
        // 日付範囲付き統計情報取得が200を返すことを検証する
        StatsResponse stats = new StatsResponse();
        stats.setPending(1);
        stats.setPublished(3);
        stats.setDeclined(0);

        when(knowledgeService.stats(eq("2026-01-01T00:00:00Z"), eq("2026-03-01T00:00:00Z"))).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/knowledge/stats")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(1));
    }
}
