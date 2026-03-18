package com.ginga.naviai.admin.controller;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.service.AdminAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auditlogs")
@Validated
public class AdminAuditController {

    private final AdminAuditService service;

    @Autowired
    public AdminAuditController(AdminAuditService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogResponse>> list(@RequestParam(value = "q", required = false) String q,
                                                                @RequestParam(value = "actor_id", required = false) String actorId,
                                                                @RequestParam(value = "target_type", required = false) String targetType,
                                                                @RequestParam(value = "target_id", required = false) String targetId,
                                                                @RequestParam(value = "from", required = false) String from,
                                                                @RequestParam(value = "to", required = false) String to,
                                                                @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                @RequestParam(value = "per_page", required = false, defaultValue = "20") Integer perPage,
                                                                @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(service.list(q, actorId, targetType, targetId, from, to, page, perPage, sort));
    }
}
