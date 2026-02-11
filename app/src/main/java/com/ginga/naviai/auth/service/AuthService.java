package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;

import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.TokenResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    TokenResponse refreshTokens(String refreshToken);
}
