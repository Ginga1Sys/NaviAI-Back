package com.ginga.naviai.admin.service;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.entity.AuditLog;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.repository.AuditLogRepository;
import com.ginga.naviai.admin.service.impl.AdminUserServiceImpl;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.UserRole;
import com.ginga.naviai.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserServiceImpl のユニットテスト
 */
@ExtendWith(MockitoExtension.class)
public class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private AdminUserServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        service = new AdminUserServiceImpl(userRepository, auditLogRepository, objectMapper);
    }

    // ========== list() ==========

    @Test
    void list_emptyResult_returnsEmptyData() {
        // 空結果が空リストを返すことを検証する
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<UserListResponse> res = service.list(null, null, null, 1, 20);

        assertTrue(res.getData().isEmpty());
        assertEquals(0, res.getMeta().getTotal());
    }

    @Test
    void list_withUsers_mapsCorrectly() {
        // ユーザー一覧が正しくマッピングされることを検証する
        User u = createUser(1L, "testuser", "Test User", UserRole.USER, true);
        Page<User> page = new PageImpl<>(List.of(u));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResponse<UserListResponse> res = service.list(null, null, null, 1, 20);

        assertEquals(1, res.getData().size());
        assertEquals("1", res.getData().get(0).getId());
        assertEquals("Test User", res.getData().get(0).getName());
        assertEquals("user", res.getData().get(0).getRole());
        assertTrue(res.getData().get(0).isActive());
    }

    @Test
    void list_invalidRole_throwsBadRequest() {
        // 不正なロールでフィルタリングすると AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class,
                () -> service.list(null, "superadmin", null, 1, 20));
    }

    @Test
    void list_invalidStatus_throwsBadRequest() {
        // 不正なステータスでフィルタリングすると AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class,
                () -> service.list(null, null, "unknown", 1, 20));
    }

    // ========== get() ==========

    @Test
    void get_found_returnsUser() {
        // 存在するユーザーが正しく返されることを検証する
        User u = createUser(1L, "admin", "Admin", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UserListResponse res = service.get("1");

        assertEquals("1", res.getId());
        assertEquals("Admin", res.getName());
        assertEquals("admin", res.getRole());
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        // 存在しないユーザーIDで AdminNotFoundException が発生することを検証する
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AdminNotFoundException.class, () -> service.get("999"));
    }

    @Test
    void get_invalidId_throwsBadRequest() {
        // 不正なID形式で AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class, () -> service.get("abc"));
    }

    @Test
    void get_blankId_throwsBadRequest() {
        // 空のIDで AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class, () -> service.get(""));
    }

    // ========== update() ==========

    @Test
    void update_changeRole_savesAndAudits() {
        // ロール変更が保存され監査ログが書かれることを検証する
        User u = createUser(1L, "user1", "User One", UserRole.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenReturn(u);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("admin");
        req.setReason("Promoted");

        UserListResponse res = service.update("1", req);

        assertEquals("admin", res.getRole());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void update_deactivateUser_savesAndAudits() {
        // ユーザー無効化が保存され監査ログが書かれることを検証する
        User u = createUser(2L, "user2", "User Two", UserRole.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenReturn(u);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setIsActive(false);
        req.setReason("Policy violation");

        UserListResponse res = service.update("2", req);

        assertFalse(res.isActive());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void update_noChanges_doesNotAudit() {
        // 変更なしの場合は監査ログを書かないことを検証する
        User u = createUser(1L, "user1", "User One", UserRole.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenReturn(u);

        UserUpdateRequest req = new UserUpdateRequest();
        // no changes specified

        service.update("1", req);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void update_invalidRole_throwsBadRequest() {
        // 不正なロールで AdminBadRequestException が発生することを検証する
        User u = createUser(1L, "user1", "User One", UserRole.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("superadmin");

        assertThrows(AdminBadRequestException.class, () -> service.update("1", req));
    }

    @Test
    void update_userNotFound_throwsNotFoundException() {
        // 存在しないユーザー更新で AdminNotFoundException が発生することを検証する
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("admin");

        assertThrows(AdminNotFoundException.class, () -> service.update("999", req));
    }

    @Test
    void update_nullRequest_throwsBadRequest() {
        // null のリクエストで AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class, () -> service.update("1", null));
    }

    @Test
    void update_sameRole_noAudit() {
        // 同じロールに更新しても監査ログが書かれないことを検証する
        User u = createUser(1L, "user1", "User One", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenReturn(u);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("admin");

        service.update("1", req);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    // ========== ヘルパー ==========

    private User createUser(Long id, String username, String displayName, UserRole role, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setEmail(username + "@ginga.info");
        u.setRole(role);
        u.setEnabled(enabled);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }
}
