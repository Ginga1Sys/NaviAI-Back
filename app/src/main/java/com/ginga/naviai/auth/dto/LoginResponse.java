package com.ginga.naviai.auth.dto;

public class LoginResponse {
    private UserResponse user;
    private String token;
    private long expiresIn;
    private String refreshToken;

    public LoginResponse(UserResponse user, String token, long expiresIn) {
        this.user = user;
        this.token = token;
        this.expiresIn = expiresIn;
    }

    public LoginResponse(UserResponse user, String token, long expiresIn, String refreshToken) {
        this.user = user;
        this.token = token;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
