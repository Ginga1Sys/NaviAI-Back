package com.ginga.naviai.admin.service;

import com.ginga.naviai.admin.dto.*;

public interface AdminKnowledgeService {
    PagedResponse<KnowledgeSummaryResponse> list(String status, String q, String authorId, String tag, String from, String to,
                                                 Integer page, Integer perPage, String sort);
    KnowledgeDetailResponse getDetail(String id);
    SimpleStatusResponse approve(String id, String note);
    SimpleStatusResponse reject(String id, String reason);
    BulkActionResultResponse bulkAction(com.ginga.naviai.admin.dto.BulkActionRequest req);
    ModerationResponse getModeration(String id);
    ModerationResponse updateModeration(String id, ModerationRequest req);
    StatsResponse stats(String from, String to);
}
