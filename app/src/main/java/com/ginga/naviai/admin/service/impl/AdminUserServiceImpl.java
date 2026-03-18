package com.ginga.naviai.admin.service.impl;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.entity.AuditLog;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.repository.AuditLogRepository;
import com.ginga.naviai.admin.service.AdminUserService;
import com.ginga.naviai.admin.util.AdminActorUtil;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.UserRole;
import com.ginga.naviai.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminUserServiceImpl(UserRepository userRepository,
                               AuditLogRepository auditLogRepository,
                               ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserListResponse> list(String q, String role, String status, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(Math.max(0, (page != null ? page : 1) - 1),
                clampPerPage(perPage),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = Specification.where(qLike(q))
                .and(roleEq(role))
                .and(statusEq(status));

        Page<User> result = userRepository.findAll(spec, pageable);

        List<UserListResponse> data = new ArrayList<>();
        for (User u : result.getContent()) {
            data.add(toDto(u));
        }

        PagedResponse<UserListResponse> p = new PagedResponse<>();
        p.setData(data);
        PagedResponse.Meta m = new PagedResponse.Meta();
        m.setPage(page != null ? page : 1);
        m.setPerPage(perPage != null ? clampPerPage(perPage) : 20);
        m.setTotal(result.getTotalElements());
        p.setMeta(m);
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse get(String id) {
        Long userId = parseUserId(id);
        User u = userRepository.findById(userId).orElseThrow(() -> new AdminNotFoundException("User not found"));
        return toDto(u);
    }

    @Override
    @Transactional
    public UserListResponse update(String id, UserUpdateRequest req) {
        if (req == null) throw new AdminBadRequestException("request body is required");
        Long userId = parseUserId(id);

        User u = userRepository.findById(userId).orElseThrow(() -> new AdminNotFoundException("User not found"));

        Map<String, Object> changes = new HashMap<>();

        if (req.getRole() != null) {
            UserRole newRole;
            try {
                newRole = UserRole.fromApiValue(req.getRole());
            } catch (Exception ex) {
                throw new AdminBadRequestException("Invalid role", Map.of("role", "invalid"));
            }
            if (newRole == null) {
                throw new AdminBadRequestException("Invalid role", Map.of("role", "invalid"));
            }
            if (u.getRole() != newRole) {
                changes.put("role_before", u.getRole() != null ? u.getRole().toApiValue() : null);
                changes.put("role_after", newRole.toApiValue());
                u.setRole(newRole);
            }
        }

        if (req.getIsActive() != null) {
            boolean newEnabled = Boolean.TRUE.equals(req.getIsActive());
            if (u.isEnabled() != newEnabled) {
                changes.put("is_active_before", u.isEnabled());
                changes.put("is_active_after", newEnabled);
                u.setEnabled(newEnabled);
            }
        }

        if (req.getReason() != null && !req.getReason().isBlank()) {
            changes.put("reason", req.getReason().trim());
        }

        u.setUpdatedAt(Instant.now());
        userRepository.save(u);

        if (!changes.isEmpty()) {
            writeAudit("USER_UPDATED", "user", String.valueOf(u.getId()), changes);
        }

        return toDto(u);
    }

    private int clampPerPage(Integer perPage) {
        if (perPage == null) return 20;
        return Math.max(1, Math.min(100, perPage));
    }

    private Long parseUserId(String id) {
        if (id == null || id.isBlank()) throw new AdminBadRequestException("id is required");
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException ex) {
            throw new AdminBadRequestException("id must be a number");
        }
    }

    private Specification<User> qLike(String q) {
        if (q == null || q.isBlank()) return null;
        String pat = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), pat),
                cb.like(cb.lower(root.get("email")), pat),
                cb.like(cb.lower(root.get("displayName")), pat)
        );
    }

    private Specification<User> roleEq(String role) {
        if (role == null || role.isBlank()) return null;
        UserRole r;
        try {
            r = UserRole.fromApiValue(role);
        } catch (Exception ex) {
            throw new AdminBadRequestException("Invalid role", Map.of("role", "invalid"));
        }
        if (r == null) throw new AdminBadRequestException("Invalid role", Map.of("role", "invalid"));
        return (root, query, cb) -> cb.equal(root.get("role"), r);
    }

    private Specification<User> statusEq(String status) {
        if (status == null || status.isBlank()) return null;
        String s = status.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "active" -> (root, query, cb) -> cb.isTrue(root.get("enabled"));
            case "inactive" -> (root, query, cb) -> cb.isFalse(root.get("enabled"));
            default -> throw new AdminBadRequestException("Invalid status", Map.of("status", "invalid"));
        };
    }

    private UserListResponse toDto(User u) {
        UserListResponse r = new UserListResponse();
        r.setId(String.valueOf(u.getId()));
        r.setName(displayName(u));
        r.setEmail(u.getEmail());
        r.setRole(u.getRole() != null ? u.getRole().toApiValue() : UserRole.USER.toApiValue());
        r.setActive(u.isEnabled());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }

    private String displayName(User u) {
        if (u.getDisplayName() != null && !u.getDisplayName().isBlank()) return u.getDisplayName();
        return u.getUsername();
    }

    private void writeAudit(String action, String targetType, String targetId, Object detail) {
        Long actorId = AdminActorUtil.getActorUserIdOrNull();
        String actorName = null;
        if (actorId != null) {
            actorName = userRepository.findById(actorId).map(this::displayName).orElse(null);
        }

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActorId(actorId);
        log.setActorName(actorName);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        try {
            log.setDetailJson(detail != null ? objectMapper.writeValueAsString(detail) : null);
        } catch (Exception ignored) {
            log.setDetailJson(null);
        }
        auditLogRepository.save(log);
    }
}
