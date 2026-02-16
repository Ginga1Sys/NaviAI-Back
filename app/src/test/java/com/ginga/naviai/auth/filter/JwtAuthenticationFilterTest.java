package com.ginga.naviai.auth.filter;

import com.ginga.naviai.auth.service.TokenBlacklistService;
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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        filter = new JwtAuthenticationFilter(tokenBlacklistService);
    }

    // ========== ブラックリストチェックのテスト ==========

    @Test
    void doFilterInternal_blacklistedJti_returns401() throws ServletException, IOException {
        // ブラックリストに登録された jti でリクエストした場合、401 が返されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-access-token");
        request.addHeader("X-Token-Jti", "blacklisted-jti");
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
        request.addHeader("Authorization", "Bearer some-access-token");
        request.addHeader("X-Token-Jti", "valid-jti");
        request.setServletPath("/api/v1/protected");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        when(tokenBlacklistService.isBlacklisted("valid-jti")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(200, response.getStatus());
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
    void doFilterInternal_noJtiHeader_continuesFilterChain() throws ServletException, IOException {
        // X-Token-Jti ヘッダがない場合、ブラックリストチェックをスキップしてフィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-access-token");
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
    void doFilterInternal_emptyJtiHeader_continuesFilterChain() throws ServletException, IOException {
        // X-Token-Jti ヘッダが空の場合、ブラックリストチェックをスキップしてフィルタチェーンが継続されることを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-access-token");
        request.addHeader("X-Token-Jti", "");
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
        // /api/v1/auth/logout はフィルタを適用することを検証する（認証が必要）
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/logout");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldNotFilter_refreshEndpoint_returnsFalse() {
        // /api/v1/auth/refresh はフィルタを適用することを検証する
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/refresh");

        // Act
        boolean result = filter.shouldNotFilter(request);

        // Assert
        assertFalse(result);
    }
}
