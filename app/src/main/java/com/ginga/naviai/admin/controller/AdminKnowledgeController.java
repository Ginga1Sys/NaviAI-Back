package com.ginga.naviai.admin.controller;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.service.AdminKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/knowledge")
@Validated
public class AdminKnowledgeController {

    private final AdminKnowledgeService service;

    @Autowired
    public AdminKnowledgeController(AdminKnowledgeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<KnowledgeSummaryResponse>> list(
            @RequestParam(value = "status", required = false, defaultValue = "pending") String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "author_id", required = false) String authorId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") Integer perPage,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        PagedResponse<KnowledgeSummaryResponse> res = service.list(status, q, authorId, tag, from, to, page, perPage, sort);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingleResponse<KnowledgeDetailResponse>> getDetail(@PathVariable("id") String id) {
        return ResponseEntity.ok(SingleResponse.of(service.getDetail(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SingleResponse<SimpleStatusResponse>> approve(@PathVariable("id") String id,
                                                                          @RequestBody(required = false) ApprovalRequest req) {
        SimpleStatusResponse r = service.approve(id, req != null ? req.getNote() : null);
        return ResponseEntity.ok(SingleResponse.of(r));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SingleResponse<SimpleStatusResponse>> reject(@PathVariable("id") String id,
                                                                         @Valid @RequestBody RejectRequest req) {
        SimpleStatusResponse r = service.reject(id, req.getReason());
        return ResponseEntity.ok(SingleResponse.of(r));
    }

    @PostMapping("/bulk-action")
    public ResponseEntity<SingleResponse<BulkActionResultResponse>> bulkAction(@Valid @RequestBody BulkActionRequest req) {
        BulkActionResultResponse result = service.bulkAction(req);
        return ResponseEntity.ok(SingleResponse.of(result));
    }

    @GetMapping("/{id}/moderation")
    public ResponseEntity<SingleResponse<ModerationResponse>> getModeration(@PathVariable("id") String id) {
        return ResponseEntity.ok(SingleResponse.of(service.getModeration(id)));
    }

    @PutMapping("/{id}/moderation")
    public ResponseEntity<SingleResponse<ModerationResponse>> putModeration(@PathVariable("id") String id,
                                                                            @Valid @RequestBody ModerationRequest req) {
        ModerationResponse r = service.updateModeration(id, req);
        return ResponseEntity.ok(SingleResponse.of(r));
    }

    @GetMapping("/stats")
    public ResponseEntity<SingleResponse<StatsResponse>> stats(@RequestParam(value = "from", required = false) String from,
                                                                @RequestParam(value = "to", required = false) String to) {
        StatsResponse r = service.stats(from, to);
        return ResponseEntity.ok(SingleResponse.of(r));
    }
}
