package com.ginga.naviai.admin.service.impl;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.entity.AuditLog;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.repository.AuditLogRepository;
import com.ginga.naviai.admin.service.AdminAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditServiceImpl(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public PagedResponse<AuditLogResponse> list(String q, String actorId, String targetType, String targetId, String from, String to,
                                               Integer page, Integer perPage, String sort) {
        Pageable pageable = PageRequest.of(Math.max(0, (page != null ? page : 1) - 1),
                clampPerPage(perPage),
                toSort(sort));

        Specification<AuditLog> spec = Specification.where(qLike(q))
                .and(actorEq(actorId))
                .and(targetTypeEq(targetType))
                .and(targetIdEq(targetId))
                .and(createdAtBetween(from, to));

        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        List<AuditLogResponse> data = new ArrayList<>();
        for (AuditLog l : result.getContent()) {
            AuditLogResponse r = new AuditLogResponse();
            r.setId(l.getId());
            r.setAction(l.getAction());
            UserBrief actor = new UserBrief();
            actor.setId(l.getActorId() != null ? String.valueOf(l.getActorId()) : "0");
            actor.setName(l.getActorName() != null ? l.getActorName() : "unknown");
            r.setActor(actor);
            AuditLogResponse.Target t = new AuditLogResponse.Target();
            t.setType(l.getTargetType());
            t.setId(l.getTargetId());
            r.setTarget(t);
            r.setCreatedAt(l.getCreatedAt());
            r.setDetail(parseDetail(l.getDetailJson()));
            data.add(r);
        }

        PagedResponse<AuditLogResponse> p = new PagedResponse<>();
        p.setData(data);
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(page != null ? page : 1);
        meta.setPerPage(perPage != null ? clampPerPage(perPage) : 20);
        meta.setTotal(result.getTotalElements());
        p.setMeta(meta);
        return p;
    }

    private Object parseDetail(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private int clampPerPage(Integer perPage) {
        if (perPage == null) return 20;
        return Math.max(1, Math.min(100, perPage));
    }

    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String s = sort.trim();
        Sort.Direction dir = s.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String key = s.startsWith("-") ? s.substring(1) : s;
        return switch (key) {
            case "created_at" -> Sort.by(dir, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Specification<AuditLog> qLike(String q) {
        if (q == null || q.isBlank()) return null;
        String pat = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("action")), pat),
                cb.like(cb.lower(root.get("actorName")), pat),
                cb.like(cb.lower(root.get("targetId")), pat)
        );
    }

    private Specification<AuditLog> actorEq(String actorId) {
        if (actorId == null || actorId.isBlank()) return null;
        try {
            Long id = Long.valueOf(actorId.trim());
            return (root, query, cb) -> cb.equal(root.get("actorId"), id);
        } catch (NumberFormatException ex) {
            throw new AdminBadRequestException("actor_id must be a number");
        }
    }

    private Specification<AuditLog> targetTypeEq(String type) {
        if (type == null || type.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("targetType"), type.trim());
    }

    private Specification<AuditLog> targetIdEq(String id) {
        if (id == null || id.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("targetId"), id.trim());
    }

    private Specification<AuditLog> createdAtBetween(String from, String to) {
        Instant fromI = parseInstant(from);
        Instant toI = parseInstant(to);
        if (fromI == null && toI == null) return null;
        return (root, query, cb) -> {
            if (fromI != null && toI != null) {
                return cb.between(root.get("createdAt"), fromI, toI);
            }
            if (fromI != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), fromI);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), toI);
        };
    }

    private Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s.trim()).toInstant();
        } catch (Exception ex) {
            throw new AdminBadRequestException("Invalid datetime format");
        }
    }
}
