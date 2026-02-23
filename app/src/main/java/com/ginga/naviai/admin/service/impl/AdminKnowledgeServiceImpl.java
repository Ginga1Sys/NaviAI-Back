package com.ginga.naviai.admin.service.impl;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.entity.AuditLog;
import com.ginga.naviai.admin.entity.KnowledgeModeration;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminConflictException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.repository.AuditLogRepository;
import com.ginga.naviai.admin.repository.KnowledgeModerationRepository;
import com.ginga.naviai.admin.util.AdminActorUtil;
import com.ginga.naviai.admin.service.AdminKnowledgeService;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.KnowledgeStatus;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

@Service
public class AdminKnowledgeServiceImpl implements AdminKnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeModerationRepository moderationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AdminKnowledgeServiceImpl(KnowledgeRepository knowledgeRepository,
                                    KnowledgeModerationRepository moderationRepository,
                                    AuditLogRepository auditLogRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this.knowledgeRepository = knowledgeRepository;
        this.moderationRepository = moderationRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<KnowledgeSummaryResponse> list(String status, String q, String authorId, String tag, String from, String to,
                                                       Integer page, Integer perPage, String sort) {
        KnowledgeStatus st = KnowledgeStatus.fromApiValue(status);
        if (st == null) st = KnowledgeStatus.PENDING;

        Pageable pageable = PageRequest.of(Math.max(0, (page != null ? page : 1) - 1),
                clampPerPage(perPage),
                toSort(sort));

        Specification<Knowledge> spec = Specification.where(statusEq(st))
                .and(qLike(q))
                .and(authorEq(authorId))
                .and(tagEq(tag))
                .and(createdAtBetween(from, to));

        Page<Knowledge> result = knowledgeRepository.findAll(spec, pageable);

        Map<Long, User> usersById = loadUsers(result.getContent());

        List<KnowledgeSummaryResponse> data = new ArrayList<>();
        for (Knowledge k : result.getContent()) {
            KnowledgeSummaryResponse r = new KnowledgeSummaryResponse();
            r.setId(k.getId());
            r.setTitle(k.getTitle());
            r.setSummary(excerpt(k.getBody(), 120));
            r.setStatus(k.getStatus().toApiValue());
            r.setCategory(k.getCategory());
            r.setSubmittedAt(k.getSubmittedAt());

            User u = usersById.get(k.getAuthorId());
            UserBrief ub = new UserBrief();
            ub.setId(String.valueOf(k.getAuthorId()));
            ub.setName(u != null ? displayName(u) : String.valueOf(k.getAuthorId()));
            r.setAuthor(ub);

            data.add(r);
        }

        PagedResponse<KnowledgeSummaryResponse> p = new PagedResponse<>();
        p.setData(data);
        PagedResponse.Meta meta = new PagedResponse.Meta();
        meta.setPage(page != null ? page : 1);
        meta.setPerPage(perPage != null ? clampPerPage(perPage) : 20);
        meta.setTotal(result.getTotalElements());
        p.setMeta(meta);
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDetailResponse getDetail(String id) {
        Knowledge k = knowledgeRepository.findById(id)
            .orElseThrow(() -> new AdminNotFoundException("Knowledge not found"));

        KnowledgeDetailResponse r = new KnowledgeDetailResponse();
        r.setId(k.getId());
        r.setTitle(k.getTitle());
        r.setBody(k.getBody());
        r.setStatus(k.getStatus().toApiValue());
        r.setTags(k.getTags());
        r.setCategory(k.getCategory());
        r.setCreatedAt(k.getCreatedAt());
        r.setUpdatedAt(k.getUpdatedAt());
        r.setSubmittedAt(k.getSubmittedAt());

        UserBrief ub = new UserBrief();
        ub.setId(String.valueOf(k.getAuthorId()));
        userRepository.findById(k.getAuthorId()).ifPresent(u -> ub.setName(displayName(u)));
        if (ub.getName() == null) ub.setName(String.valueOf(k.getAuthorId()));
        r.setAuthor(ub);
        return r;
    }

    @Override
    @Transactional
    public SimpleStatusResponse approve(String id, String note) {
        Knowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Knowledge not found"));
        if (k.getStatus() != KnowledgeStatus.PENDING) {
            throw new AdminConflictException("Only pending knowledge can be approved");
        }
        k.setStatus(KnowledgeStatus.PUBLISHED);
        k.setPublishedAt(Instant.now());
        knowledgeRepository.save(k);

        writeAudit("KNOWLEDGE_APPROVED", "knowledge", k.getId(), Map.of(
                "title", k.getTitle(),
                "note", note
        ));

        SimpleStatusResponse r = new SimpleStatusResponse();
        r.setId(k.getId());
        r.setStatus(k.getStatus().toApiValue());
        r.setPublishedAt(k.getPublishedAt() != null ? k.getPublishedAt().toString() : null);
        return r;
    }

    @Override
    @Transactional
    public SimpleStatusResponse reject(String id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new AdminBadRequestException("reason is required", Map.of("reason", "must not be blank"));
        }
        Knowledge k = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Knowledge not found"));
        if (k.getStatus() != KnowledgeStatus.PENDING) {
            throw new AdminConflictException("Only pending knowledge can be rejected");
        }
        k.setStatus(KnowledgeStatus.DECLINED);
        k.setDeclinedReason(reason);
        knowledgeRepository.save(k);

        writeAudit("KNOWLEDGE_REJECTED", "knowledge", k.getId(), Map.of(
                "title", k.getTitle(),
                "reason", reason
        ));

        SimpleStatusResponse r = new SimpleStatusResponse();
        r.setId(k.getId());
        r.setStatus(k.getStatus().toApiValue());
        return r;
    }

    @Override
    @Transactional
    public BulkActionResultResponse bulkAction(BulkActionRequest req) {
        if (req.getAction() == null || req.getAction().trim().isEmpty()) {
            throw new AdminBadRequestException("action is required");
        }
        if (req.getIds() == null || req.getIds().isEmpty()) {
            throw new AdminBadRequestException("ids is required");
        }

        String action = req.getAction().trim().toLowerCase(Locale.ROOT);
        if (!action.equals("approve") && !action.equals("reject")) {
            throw new AdminBadRequestException("Unknown action");
        }
        if (action.equals("reject") && (req.getReason() == null || req.getReason().trim().isEmpty())) {
            throw new AdminBadRequestException("reason is required for reject", Map.of("reason", "must not be blank"));
        }

        List<BulkActionResultResponse.BulkItemResult> results = new ArrayList<>();
        int ok = 0;
        int failed = 0;

        for (String id : req.getIds()) {
            BulkActionResultResponse.BulkItemResult item = new BulkActionResultResponse.BulkItemResult();
            item.setId(id);
            try {
                if (action.equals("approve")) {
                    approve(id, null);
                } else {
                    reject(id, req.getReason());
                }
                item.setOk(true);
                ok++;
            } catch (Exception ex) {
                item.setOk(false);
                BulkActionResultResponse.ErrorDetail ed = new BulkActionResultResponse.ErrorDetail();
                ed.setCode(ex instanceof com.ginga.naviai.admin.exception.AdminApiException a ? a.getCode() : "ERROR");
                ed.setMessage(ex.getMessage() == null ? "Error" : ex.getMessage());
                item.setError(ed);
                failed++;
            }
            results.add(item);
        }

        BulkActionResultResponse res = new BulkActionResultResponse();
        res.setAction(action);
        res.setResults(results);
        BulkActionResultResponse.Summary s = new BulkActionResultResponse.Summary();
        s.setOk(ok);
        s.setFailed(failed);
        res.setSummary(s);
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationResponse getModeration(String id) {
        knowledgeRepository.findById(id).orElseThrow(() -> new AdminNotFoundException("Knowledge not found"));
        KnowledgeModeration m = moderationRepository.findById(id)
            .orElseThrow(() -> new AdminNotFoundException("Moderation not found"));
        ModerationResponse r = new ModerationResponse();
        r.setKnowledgeId(m.getKnowledgeId());
        r.setInternalNote(m.getInternalNote());
        r.setUpdatedAt(m.getUpdatedAt());

        UserBrief ub = new UserBrief();
        if (m.getUpdatedBy() != null) {
            ub.setId(String.valueOf(m.getUpdatedBy()));
            userRepository.findById(m.getUpdatedBy()).ifPresent(u -> ub.setName(displayName(u)));
            if (ub.getName() == null) ub.setName(String.valueOf(m.getUpdatedBy()));
        } else {
            ub.setId("0");
            ub.setName("unknown");
        }
        r.setUpdatedBy(ub);
        return r;
    }

    @Override
    @Transactional
    public ModerationResponse updateModeration(String id, ModerationRequest req) {
        knowledgeRepository.findById(id).orElseThrow(() -> new AdminNotFoundException("Knowledge not found"));
        KnowledgeModeration m = moderationRepository.findById(id).orElseGet(KnowledgeModeration::new);
        m.setKnowledgeId(id);
        m.setInternalNote(req.getInternalNote());
        Long actorId = AdminActorUtil.getActorUserIdOrNull();
        m.setUpdatedBy(actorId);
        m.setUpdatedAt(Instant.now());
        moderationRepository.save(m);

        writeAudit("KNOWLEDGE_MODERATION_UPDATED", "knowledge", id, Map.of(
                "internal_note", req.getInternalNote()
        ));

        return getModeration(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse stats(String from, String to) {
        Specification<Knowledge> dateSpec = createdAtBetween(from, to);
        StatsResponse s = new StatsResponse();
        s.setPending((int) knowledgeRepository.count(Specification.where(statusEq(KnowledgeStatus.PENDING)).and(dateSpec)));
        s.setPublished((int) knowledgeRepository.count(Specification.where(statusEq(KnowledgeStatus.PUBLISHED)).and(dateSpec)));
        s.setDeclined((int) knowledgeRepository.count(Specification.where(statusEq(KnowledgeStatus.DECLINED)).and(dateSpec)));
        return s;
    }

    private int clampPerPage(Integer perPage) {
        if (perPage == null) return 20;
        return Math.max(1, Math.min(100, perPage));
    }

    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "submittedAt");
        String s = sort.trim();
        Sort.Direction dir = s.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String key = s.startsWith("-") ? s.substring(1) : s;
        return switch (key) {
            case "submitted_at" -> Sort.by(dir, "submittedAt");
            case "created_at" -> Sort.by(dir, "createdAt");
            case "updated_at" -> Sort.by(dir, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "submittedAt");
        };
    }

    private Specification<Knowledge> statusEq(KnowledgeStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<Knowledge> qLike(String q) {
        if (q == null || q.isBlank()) return null;
        String pat = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pat),
                cb.like(cb.lower(root.get("body")), pat)
        );
    }

    private Specification<Knowledge> authorEq(String authorId) {
        if (authorId == null || authorId.isBlank()) return null;
        try {
            Long id = Long.valueOf(authorId.trim());
            return (root, query, cb) -> cb.equal(root.get("authorId"), id);
        } catch (NumberFormatException ex) {
            throw new AdminBadRequestException("author_id must be a number");
        }
    }

    private Specification<Knowledge> tagEq(String tag) {
        if (tag == null || tag.isBlank()) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> join = root.join("tags", JoinType.LEFT);
            return cb.equal(join, tag.trim());
        };
    }

    private Specification<Knowledge> createdAtBetween(String from, String to) {
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

    private Map<Long, User> loadUsers(List<Knowledge> knowledgeList) {
        Set<Long> ids = new HashSet<>();
        for (Knowledge k : knowledgeList) {
            if (k.getAuthorId() != null) ids.add(k.getAuthorId());
        }
        Map<Long, User> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (User u : userRepository.findAllById(ids)) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private String displayName(User u) {
        if (u.getDisplayName() != null && !u.getDisplayName().isBlank()) return u.getDisplayName();
        return u.getUsername();
    }

    private String excerpt(String body, int max) {
        if (body == null) return null;
        String b = body.replaceAll("\\s+", " ").trim();
        if (b.length() <= max) return b;
        return b.substring(0, max) + "...";
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
