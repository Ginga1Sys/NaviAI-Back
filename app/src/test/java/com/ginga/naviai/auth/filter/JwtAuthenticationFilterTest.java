package com.ginga.naviai.auth.filter;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import com.ginga.naviai.auth.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    private static final String TOKEN_SECRET = "test-secret-key-for-hashing-tokens-minimum-32-chars";

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        filter = new JwtAuthenticationFilter(tokenBlacklistService);
        ReflectionTestUtils.setField(filter, "tokenSecret", TOKEN_SECRET);
        SecurityContextHolder.clearContext();
    }

    // ========== ブラックリストチェックのテスト ==========

    @Test
    void doFilterInternal_blacklistedJti_returns401() throws ServletException, IOException {
        // ブラックリストに登録された jti でリクエストした場合、401 が返されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "blacklisted-jti", 3600L, TOKEN_SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        when(tokenBlacklistService.isBlacklisted("blacklisted-jti")).thenReturn(true);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token has been revoked"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validJti_continuesFilterChain() throws ServletException, IOException {
        // 有効な jti でリクエストした場合、フィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "valid-jti", 3600L, TOKEN_SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        when(tokenBlacklistService.isBlacklisted("valid-jti")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("1", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noAuthHeader_continuesFilterChain() throws ServletException, IOException {
        // Authorization ヘッダがない場合、フィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noJtiHeader_usesTokenJti() throws ServletException, IOException {
        // X-Token-Jti ヘッダがない場合でも JWT の jti を使ってブラックリストチェックが行われることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "valid-jti", 3600L, TOKEN_SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isBlacklisted("valid-jti")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        verify(tokenBlacklistService, times(1)).isBlacklisted("valid-jti");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_emptyBearerToken_continuesFilterChain() throws ServletException, IOException {
        // Bearer の値が空の場合、フィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_nonBearerAuth_continuesFilterChain() throws ServletException, IOException {
        // Bearer 以外の認証方式の場合、フィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        request.addHeader("X-Token-Jti", "some-jti");
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_returns401() throws ServletException, IOException {
        // 不正な JWT の場合、401 が返されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        request.setServletPath("/api/v1/protected");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid access token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidSignature_returns401() throws ServletException, IOException {
        // 署名不正の JWT の場合、401 が返されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "sig-jti", 3600L, "other-secret-key-for-test-minimum-32-chars");
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/v1/protected");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid access token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_prefersTokenJti_overHeaderJti() throws ServletException, IOException {
        // JWT の jti を優先し、ヘッダの jti を無視することを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "token-jti", 3600L, TOKEN_SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("X-Token-Jti", "header-jti");
        request.setServletPath("/api/v1/protected");

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isBlacklisted("token-jti")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
        verify(tokenBlacklistService, times(1)).isBlacklisted("token-jti");
        verify(tokenBlacklistService, never()).isBlacklisted("header-jti");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_expiredToken_returns401() throws ServletException, IOException {
        // 期限切れの JWT の場合、401 が返されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JwtTokenUtil.generateAccessToken("1", "expired-jti", -1L, TOKEN_SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/v1/protected");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid access token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    // ========== shouldNotFilter テスト ==========

    @Test
    void shouldNotFilter_registerEndpoint_returnsTrue() {
        // /api/v1/auth/register はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/register");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_loginEndpoint_returnsTrue() {
        // /api/v1/auth/login はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_confirmEndpoint_returnsTrue() {
        // /api/v1/auth/confirm はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/confirm");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_h2Console_returnsTrue() {
        // /h2-console はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/h2-console");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_protectedEndpoint_returnsFalse() {
        // 保護されたエンドポイントはフィルタを適用することを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/articles");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldNotFilter_logoutEndpoint_returnsFalse() {
        // /api/v1/auth/logout はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/logout");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_refreshEndpoint_returnsFalse() {
        // /api/v1/auth/refresh はフィルタをスキップすることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/refresh");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertTrue(result);
    }
}
