package com.ginga.naviai.auth.controller;

import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.dto.RefreshRequest;
import com.ginga.naviai.auth.dto.TokenResponse;
import com.ginga.naviai.auth.dto.LogoutRequest;
import com.ginga.naviai.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ginga.naviai.auth.service.ConfirmationTokenService;
import com.ginga.naviai.auth.entity.ConfirmationToken;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.auth.entity.User;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final ConfirmationTokenService tokenService;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(AuthService authService, ConfirmationTokenService tokenService, UserRepository userRepository) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse res = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse res = authService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirmEmail(@RequestParam("token") String token) {
        Optional<ConfirmationToken> opt = tokenService.findByToken(token);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid token");
        }
        ConfirmationToken ct = opt.get();
        if (ct.getConfirmedAt() != null) {
            return ResponseEntity.ok("already confirmed");
        }
        if (ct.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("token expired");
        }
        tokenService.setConfirmedAt(ct, Instant.now());
        // enable user and persist
        User u = ct.getUser();
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok("confirmed");
    }

    /**
     * ログアウトエンドポイント
     * リフレッシュトークンを無効化し、ユーザーをログアウトする
     * 
     * 注意: 現在の実装では簡易的にユーザー名をリクエストパラメータから取得しています。
     * 本番環境では、アクセストークンを検証して SecurityContext からユーザー情報を取得することを推奨します。
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) LogoutRequest request,
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "jti", required = false) String jti) {
        
        // 空のユーザー名を拒否
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(username, Optional.ofNullable(refreshToken), Optional.ofNullable(jti));
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
