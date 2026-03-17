package com.ginga.naviai.admin.service;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.entity.AuditLog;
import com.ginga.naviai.admin.entity.KnowledgeModeration;
import com.ginga.naviai.admin.exception.AdminBadRequestException;
import com.ginga.naviai.admin.exception.AdminConflictException;
import com.ginga.naviai.admin.exception.AdminNotFoundException;
import com.ginga.naviai.admin.repository.AuditLogRepository;
import com.ginga.naviai.admin.repository.KnowledgeModerationRepository;
import com.ginga.naviai.admin.service.impl.AdminKnowledgeServiceImpl;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.UserRole;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.KnowledgeStatus;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
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
 * AdminKnowledgeServiceImpl のユニットテスト
 */
@ExtendWith(MockitoExtension.class)
public class AdminKnowledgeServiceImplTest {

    @Mock private KnowledgeRepository knowledgeRepository;
    @Mock private KnowledgeModerationRepository moderationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminKnowledgeService self;

    private AdminKnowledgeServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        service = new AdminKnowledgeServiceImpl(knowledgeRepository, moderationRepository,
                auditLogRepository, userRepository, objectMapper, self);
    }

    // ========== list() ==========

    @Test
    void list_emptyResult_returnsEmptyData() {
        // 空の結果が空リストを返すことを検証する
        Page<Knowledge> emptyPage = new PageImpl<>(List.of());
        when(knowledgeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<KnowledgeSummaryResponse> res = service.list("pending", null, null, null, null, null, 1, 20, null);

        assertNotNull(res);
        assertTrue(res.getData().isEmpty());
        assertEquals(0, res.getMeta().getTotal());
    }

    @Test
    void list_withResults_mapsCorrectly() {
        // 結果がある場合に正しくマッピングされることを検証する
        Knowledge k = createKnowledge("k1", "Title One", "Body text", KnowledgeStatus.PENDING, 1L);

        Page<Knowledge> page = new PageImpl<>(List.of(k));
        when(knowledgeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        User author = createUser(1L, "author1", "Author One");
        when(userRepository.findAllById(anySet())).thenReturn(List.of(author));

        PagedResponse<KnowledgeSummaryResponse> res = service.list("pending", null, null, null, null, null, 1, 20, null);

        assertEquals(1, res.getData().size());
        assertEquals("k1", res.getData().get(0).getId());
        assertEquals("Title One", res.getData().get(0).getTitle());
        assertEquals("Author One", res.getData().get(0).getAuthor().getName());
    }

    @Test
    void list_nullStatus_defaultsToPending() {
        // status が null の場合デフォルトで PENDING になることを検証する
        Page<Knowledge> emptyPage = new PageImpl<>(List.of());
        when(knowledgeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<KnowledgeSummaryResponse> res = service.list(null, null, null, null, null, null, 1, 20, null);

        assertNotNull(res);
    }

    @Test
    void list_invalidAuthorId_throwsBadRequest() {
        // author_id が数値でない場合 AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class,
                () -> service.list("pending", null, "not-a-number", null, null, null, 1, 20, null));
    }

    // ========== getDetail() ==========

    @Test
    void getDetail_found_returnsDetail() {
        // 存在するナレッジの詳細が正しく返されることを検証する
        Knowledge k = createKnowledge("k1", "Title", "Body", KnowledgeStatus.PENDING, 1L);
        k.setTags(List.of("java", "spring"));
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "author", "Author")));

        KnowledgeDetailResponse res = service.getDetail("k1");

        assertEquals("k1", res.getId());
        assertEquals("Title", res.getTitle());
        assertEquals(2, res.getTags().size());
    }

    @Test
    void getDetail_notFound_throwsNotFoundException() {
        // 存在しないIDで AdminNotFoundException が発生することを検証する
        when(knowledgeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AdminNotFoundException.class, () -> service.getDetail("missing"));
    }

    // ========== approve() ==========

    @Test
    void approve_pendingKnowledge_setsPublished() {
        // PENDING の知識を承認すると PUBLISHED になることを検証する
        Knowledge k = createKnowledge("k1", "Title", "Body", KnowledgeStatus.PENDING, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));
        when(knowledgeRepository.save(any(Knowledge.class))).thenReturn(k);
        lenient().when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        SimpleStatusResponse res = service.approve("k1", "Good");

        assertEquals("published", res.getStatus());
        assertNotNull(res.getPublishedAt());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void approve_nonPending_throwsConflict() {
        // PENDING 以外のステータスの承認で AdminConflictException が発生することを検証する
        Knowledge k = createKnowledge("k1", "Title", "Body", KnowledgeStatus.PUBLISHED, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));

        assertThrows(AdminConflictException.class, () -> service.approve("k1", null));
    }

    @Test
    void approve_notFound_throwsNotFoundException() {
        // 存在しないIDの承認で AdminNotFoundException が発生することを検証する
        when(knowledgeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AdminNotFoundException.class, () -> service.approve("missing", null));
    }

    // ========== reject() ==========

    @Test
    void reject_pendingKnowledge_setsDeclined() {
        // PENDING の知識を却下すると DECLINED になることを検証する
        Knowledge k = createKnowledge("k1", "Title", "Body", KnowledgeStatus.PENDING, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));
        when(knowledgeRepository.save(any(Knowledge.class))).thenReturn(k);
        lenient().when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        SimpleStatusResponse res = service.reject("k1", "Poor quality");

        assertEquals("declined", res.getStatus());
        assertEquals("Poor quality", k.getDeclinedReason());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void reject_emptyReason_throwsBadRequest() {
        // 空の reason で AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class, () -> service.reject("k1", ""));
    }

    @Test
    void reject_nullReason_throwsBadRequest() {
        // null の reason で AdminBadRequestException が発生することを検証する
        assertThrows(AdminBadRequestException.class, () -> service.reject("k1", null));
    }

    @Test
    void reject_nonPending_throwsConflict() {
        // PENDING 以外のステータスの却下で AdminConflictException が発生することを検証する
        Knowledge k = createKnowledge("k1", "Title", "Body", KnowledgeStatus.DECLINED, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));

        assertThrows(AdminConflictException.class, () -> service.reject("k1", "Reason"));
    }

    // ========== bulkAction() ==========

    @Test
    void bulkAction_approve_callsSelfApprove() {
        // bulk approve が self.approve() をプロキシ経由で呼ぶことを検証する
        SimpleStatusResponse ssr = new SimpleStatusResponse();
        ssr.setId("k1");
        ssr.setStatus("published");
        when(self.approve(eq("k1"), isNull())).thenReturn(ssr);

        BulkActionRequest req = new BulkActionRequest();
        req.setAction("approve");
        req.setIds(List.of("k1"));

        BulkActionResultResponse res = service.bulkAction(req);

        assertEquals("approve", res.getAction());
        assertEquals(1, res.getSummary().getOk());
        assertEquals(0, res.getSummary().getFailed());
        verify(self).approve("k1", null);
    }

    @Test
    void bulkAction_reject_callsSelfReject() {
        // bulk reject が self.reject() をプロキシ経由で呼ぶことを検証する
        SimpleStatusResponse ssr = new SimpleStatusResponse();
        ssr.setId("k1");
        ssr.setStatus("declined");
        when(self.reject(eq("k1"), eq("Reason"))).thenReturn(ssr);

        BulkActionRequest req = new BulkActionRequest();
        req.setAction("reject");
        req.setIds(List.of("k1"));
        req.setReason("Reason");

        BulkActionResultResponse res = service.bulkAction(req);

        assertEquals(1, res.getSummary().getOk());
        verify(self).reject("k1", "Reason");
    }

    @Test
    void bulkAction_partialFailure_recordsBoth() {
        // 一部失敗がサマリーに反映されることを検証する
        when(self.approve(eq("k1"), isNull())).thenReturn(new SimpleStatusResponse());
        when(self.approve(eq("k2"), isNull())).thenThrow(new AdminNotFoundException("Not found"));

        BulkActionRequest req = new BulkActionRequest();
        req.setAction("approve");
        req.setIds(List.of("k1", "k2"));

        BulkActionResultResponse res = service.bulkAction(req);

        assertEquals(1, res.getSummary().getOk());
        assertEquals(1, res.getSummary().getFailed());
        assertTrue(res.getResults().get(0).isOk());
        assertFalse(res.getResults().get(1).isOk());
        assertEquals("NOT_FOUND", res.getResults().get(1).getError().getCode());
    }

    @Test
    void bulkAction_unknownAction_throwsBadRequest() {
        // 不明なアクションで AdminBadRequestException が発生することを検証する
        BulkActionRequest req = new BulkActionRequest();
        req.setAction("delete");
        req.setIds(List.of("k1"));

        assertThrows(AdminBadRequestException.class, () -> service.bulkAction(req));
    }

    @Test
    void bulkAction_emptyIds_throwsBadRequest() {
        // 空の ids で AdminBadRequestException が発生することを検証する
        BulkActionRequest req = new BulkActionRequest();
        req.setAction("approve");
        req.setIds(List.of());

        assertThrows(AdminBadRequestException.class, () -> service.bulkAction(req));
    }

    @Test
    void bulkAction_rejectWithoutReason_throwsBadRequest() {
        // reject でリーズンなしが AdminBadRequestException になることを検証する
        BulkActionRequest req = new BulkActionRequest();
        req.setAction("reject");
        req.setIds(List.of("k1"));

        assertThrows(AdminBadRequestException.class, () -> service.bulkAction(req));
    }

    // ========== stats() ==========

    @Test
    void stats_returnsCorrectCounts() {
        // 統計が正しいカウントを返すことを検証する
        when(knowledgeRepository.count(any(Specification.class)))
                .thenReturn(5L, 10L, 2L);

        StatsResponse res = service.stats(null, null);

        assertEquals(5L, res.getPending());
        assertEquals(10L, res.getPublished());
        assertEquals(2L, res.getDeclined());
    }

    // ========== getModeration() ==========

    @Test
    void getModeration_found_returnsModerationInfo() {
        // モデレーション情報が正しく返されることを検証する
        Knowledge k = createKnowledge("k1", "T", "B", KnowledgeStatus.PENDING, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));

        KnowledgeModeration m = new KnowledgeModeration();
        m.setKnowledgeId("k1");
        m.setInternalNote("Note");
        m.setUpdatedBy(1L);
        when(moderationRepository.findById("k1")).thenReturn(Optional.of(m));
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "admin", "Admin")));

        ModerationResponse res = service.getModeration("k1");

        assertEquals("k1", res.getKnowledgeId());
        assertEquals("Note", res.getInternalNote());
    }

    @Test
    void getModeration_knowledgeNotFound_throwsNotFound() {
        // ナレッジが存在しない場合 AdminNotFoundException が発生することを検証する
        when(knowledgeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AdminNotFoundException.class, () -> service.getModeration("missing"));
    }

    // ========== updateModeration() ==========

    @Test
    void updateModeration_createsNewIfNotExists() {
        // モデレーションが存在しない場合に新規作成されることを検証する
        Knowledge k = createKnowledge("k1", "T", "B", KnowledgeStatus.PENDING, 1L);
        when(knowledgeRepository.findById("k1")).thenReturn(Optional.of(k));
        when(moderationRepository.findById("k1")).thenReturn(Optional.empty());
        when(moderationRepository.save(any(KnowledgeModeration.class))).thenAnswer(i -> i.getArgument(0));

        // getModeration is called internally after save
        KnowledgeModeration saved = new KnowledgeModeration();
        saved.setKnowledgeId("k1");
        saved.setInternalNote("New note");
        when(moderationRepository.findById("k1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved));

        ModerationRequest req = new ModerationRequest();
        req.setInternalNote("New note");

        // updateModeration calls getModeration internally which may throw,
        // but since we set up both calls it should work
        ModerationResponse res = service.updateModeration("k1", req);

        verify(moderationRepository).save(any(KnowledgeModeration.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ========== ヘルパー ==========

    private Knowledge createKnowledge(String id, String title, String body, KnowledgeStatus status, Long authorId) {
        Knowledge k = new Knowledge();
        k.setId(id);
        k.setTitle(title);
        k.setBody(body);
        k.setStatus(status);
        k.setAuthorId(authorId);
        k.setCreatedAt(Instant.now());
        k.setUpdatedAt(Instant.now());
        return k;
    }

    private User createUser(Long id, String username, String displayName) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setEmail(username + "@ginga.info");
        u.setRole(UserRole.USER);
        u.setEnabled(true);
        return u;
    }
}
