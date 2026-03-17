package com.ginga.naviai.admin.controller;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.service.AdminAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminAuditController のユニットテスト
 */
@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuditService auditService;

    @MockBean
    private com.ginga.naviai.auth.service.TokenBlacklistService tokenBlacklistService;

    @Test
    void list_defaultParams_returns200() throws Exception {
        // デフォルトパラメータで監査ログ一覧が200を返すことを検証する
        PagedResponse<AuditLogResponse> res = new PagedResponse<>();
        res.setData(List.of());
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(20);
        meta.setTotal(0);
        res.setMeta(meta);

        when(auditService.list(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq(20), isNull())).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/auditlogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void list_withFilters_returns200() throws Exception {
        // フィルタ付きで監査ログ一覧が200を返すことを検証する
        AuditLogResponse log = new AuditLogResponse();
        log.setId("log1");
        log.setAction("KNOWLEDGE_APPROVED");
        log.setCreatedAt(Instant.parse("2026-03-01T10:00:00Z"));

        UserBrief actor = new UserBrief();
        actor.setId("1");
        actor.setName("Admin");
        log.setActor(actor);

        AuditLogResponse.Target target = new AuditLogResponse.Target();
        target.setType("knowledge");
        target.setId("k1");
        log.setTarget(target);

        PagedResponse<AuditLogResponse> res = new PagedResponse<>();
        res.setData(List.of(log));
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(10);
        meta.setTotal(1);
        res.setMeta(meta);

        when(auditService.list(isNull(), eq("1"), eq("knowledge"), isNull(),
                isNull(), isNull(), eq(1), eq(10), eq("-created_at"))).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/auditlogs")
                        .param("actor_id", "1")
                        .param("target_type", "knowledge")
                        .param("per_page", "10")
                        .param("sort", "-created_at"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("log1"))
                .andExpect(jsonPath("$.data[0].action").value("KNOWLEDGE_APPROVED"));
    }

    @Test
    void list_withDateRange_returns200() throws Exception {
        // 日付範囲フィルタ付きで監査ログ一覧が200を返すことを検証する
        PagedResponse<AuditLogResponse> res = new PagedResponse<>();
        res.setData(List.of());
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(20);
        meta.setTotal(0);
        res.setMeta(meta);

        when(auditService.list(isNull(), isNull(), isNull(), isNull(),
                eq("2026-01-01T00:00:00Z"), eq("2026-03-01T00:00:00Z"),
                eq(1), eq(20), isNull())).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/auditlogs")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void list_withSearchQuery_returns200() throws Exception {
        // 検索クエリ付きで監査ログ一覧が200を返すことを検証する
        PagedResponse<AuditLogResponse> res = new PagedResponse<>();
        res.setData(List.of());
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(20);
        meta.setTotal(0);
        res.setMeta(meta);

        when(auditService.list(eq("APPROVED"), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(1), eq(20), isNull())).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/auditlogs")
                        .param("q", "APPROVED"))
                .andExpect(status().isOk());
    }
}
