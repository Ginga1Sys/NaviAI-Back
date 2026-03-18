package com.ginga.naviai.admin.controller;

import com.ginga.naviai.admin.dto.*;
import com.ginga.naviai.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@Validated
public class AdminUserController {

    private final AdminUserService service;

    @Autowired
    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<PagedResponse<UserListResponse>> list(@RequestParam(value = "q", required = false) String q,
                                                                 @RequestParam(value = "role", required = false) String role,
                                                                 @RequestParam(value = "status", required = false) String status,
                                                                 @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                 @RequestParam(value = "per_page", required = false, defaultValue = "20") Integer perPage) {
        return ResponseEntity.ok(service.list(q, role, status, page, perPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingleResponse<UserListResponse>> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(SingleResponse.of(service.get(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SingleResponse<UserListResponse>> patch(@PathVariable("id") String id,
                                                                   @Valid @RequestBody UserUpdateRequest req) {
        return ResponseEntity.ok(SingleResponse.of(service.update(id, req)));
    }
}
