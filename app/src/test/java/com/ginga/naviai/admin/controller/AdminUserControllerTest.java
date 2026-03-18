package com.ginga.naviai.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.service.AdminUserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController のユニットテスト
 */
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService userService;

    @MockBean
    private com.ginga.naviai.auth.service.TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ObjectMapper mapper;

    // ========== GET /api/v1/admin/users ==========

    @Test
    void list_defaultParams_returns200() throws Exception {
        // デフォルトパラメータでユーザー一覧が200を返すことを検証する
        PagedResponse<UserListResponse> res = new PagedResponse<>();
        res.setData(List.of());
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(20);
        meta.setTotal(0);
        res.setMeta(meta);

        when(userService.list(isNull(), isNull(), isNull(), eq(1), eq(20))).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void list_withFilters_returns200() throws Exception {
        // フィルタ付きでユーザー一覧が200を返すことを検証する
        UserListResponse user = new UserListResponse();
        user.setId("1");
        user.setName("TestUser");
        user.setEmail("test@ginga.info");
        user.setRole("admin");
        user.setActive(true);

        PagedResponse<UserListResponse> res = new PagedResponse<>();
        res.setData(List.of(user));
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(1);
        meta.setPerPage(10);
        meta.setTotal(1);
        res.setMeta(meta);

        when(userService.list(eq("test"), eq("admin"), eq("active"), eq(1), eq(10))).thenReturn(res);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("q", "test")
                        .param("role", "admin")
                        .param("status", "active")
                        .param("per_page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("TestUser"))
                .andExpect(jsonPath("$.data[0].role").value("admin"));
    }

    // ========== GET /api/v1/admin/users/{id} ==========

    @Test
    void get_found_returns200() throws Exception {
        // 存在するユーザーの取得が200を返すことを検証する
        UserListResponse user = new UserListResponse();
        user.setId("1");
        user.setName("Admin");
        user.setEmail("admin@ginga.info");
        user.setRole("admin");
        user.setActive(true);
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(userService.get("1")).thenReturn(user);

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.name").value("Admin"))
                .andExpect(jsonPath("$.data.email").value("admin@ginga.info"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        // 存在しないユーザーの取得が404を返すことを検証する
        when(userService.get("999")).thenThrow(new AdminNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/admin/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void get_invalidId_returns400() throws Exception {
        // 不正なIDでユーザー取得が400を返すことを検証する
        when(userService.get("abc")).thenThrow(new AdminBadRequestException("id must be a number"));

        mockMvc.perform(get("/api/v1/admin/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // ========== PATCH /api/v1/admin/users/{id} ==========

    @Test
    void patch_updateRole_returns200() throws Exception {
        // ロール変更が200を返すことを検証する
        UserListResponse updated = new UserListResponse();
        updated.setId("1");
        updated.setName("User");
        updated.setRole("admin");
        updated.setActive(true);

        when(userService.update(eq("1"), any(UserUpdateRequest.class))).thenReturn(updated);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("admin");
        req.setReason("Promoted to admin");

        mockMvc.perform(patch("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    void patch_deactivateUser_returns200() throws Exception {
        // ユーザー無効化が200を返すことを検証する
        UserListResponse updated = new UserListResponse();
        updated.setId("2");
        updated.setName("User2");
        updated.setActive(false);

        when(userService.update(eq("2"), any(UserUpdateRequest.class))).thenReturn(updated);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setIsActive(false);
        req.setReason("Policy violation");

        mockMvc.perform(patch("/api/v1/admin/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_active").value(false));
    }

    @Test
    void patch_invalidRole_returns400() throws Exception {
        // 不正なロール指定が400を返すことを検証する
        when(userService.update(eq("1"), any(UserUpdateRequest.class)))
                .thenThrow(new AdminBadRequestException("Invalid role", Map.of("role", "invalid")));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("superadmin");

        mockMvc.perform(patch("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void patch_userNotFound_returns404() throws Exception {
        // 存在しないユーザー更新が404を返すことを検証する
        when(userService.update(eq("999"), any(UserUpdateRequest.class)))
                .thenThrow(new AdminNotFoundException("User not found"));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("user");

        mockMvc.perform(patch("/api/v1/admin/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
