package com.ginga.naviai.user.controller;

import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId;
        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse res = userService.getCurrentUser(userId);
        boolean isAdminByAuthority = authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        // 現状の JWT フィルタは ROLE_USER 固定のため、管理者初期ユーザーをフォールバック判定する。
        boolean isAdminBySeedUser = "admin".equalsIgnoreCase(res.getUsername())
            || "admin@naviai.com".equalsIgnoreCase(res.getEmail());
        boolean isAdmin = isAdminByAuthority || isAdminBySeedUser;
        res.setAdmin(isAdmin);
        return ResponseEntity.ok(res);
    }
}
