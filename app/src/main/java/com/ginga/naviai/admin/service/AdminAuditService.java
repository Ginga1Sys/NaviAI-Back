package com.ginga.naviai.admin.service;

import com.ginga.naviai.admin.dto.*;

public interface AdminAuditService {
    PagedResponse<AuditLogResponse> list(String q, String actorId, String targetType, String targetId, String from, String to,
                                         Integer page, Integer perPage, String sort);
}
