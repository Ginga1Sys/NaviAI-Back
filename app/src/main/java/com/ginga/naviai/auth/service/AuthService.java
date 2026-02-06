package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;

import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.TokenResponse;

import java.util.Optional;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    TokenResponse refreshTokens(String refreshToken);
    
    /**
     * ユーザーをログアウトし、リフレッシュトークンを無効化する
     * @param username ログアウト対象のユーザー名
     * @param refreshToken 無効化するリフレッシュトークン（オプショナル）
     * @param accessTokenJti アクセストークンの jti（オプショナル、Redis ブラックリスト用）
     */
    void logout(String username, Optional<String> refreshToken, Optional<String> accessTokenJti);
}
