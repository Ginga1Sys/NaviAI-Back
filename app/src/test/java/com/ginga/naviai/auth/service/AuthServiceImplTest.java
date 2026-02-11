package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.dto.TokenResponse;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.entity.RefreshToken;
import com.ginga.naviai.auth.exception.AccountNotEnabledException;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.exception.InvalidCredentialsException;
import com.ginga.naviai.auth.exception.InvalidTokenException;
import com.ginga.naviai.auth.exception.TokenExpiredException;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.auth.repository.RefreshTokenRepository;
import com.ginga.naviai.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ConfirmationTokenService tokenService;

    @Mock
    private MailService mailService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        // Set properties for token handling
        ReflectionTestUtils.setField(authService, "tokenSecret", "test-secret-key-for-hashing-tokens-minimum-32-chars");
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 2592000L);
    }

    @Test
    void register_success_createsUserAndSendsEmail() {
        // 正常な登録処理：エンコードされたパスワードでユーザーが保存され、`enabled` が false に設定され、トークン生成とメール送信が行われることを検証する
        // Arrange: no existing username/email
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@ginga.info")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("P@ssw0rd1")).thenReturn("hashedpwd");
        when(tokenService.createTokenForUser(any(User.class))).thenReturn("token-123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            u.setCreatedAt(Instant.now());
            return u;
        });

        RegisterRequest req = new RegisterRequest();
        req.setUsername("user1");
        req.setEmail("test@ginga.info");
        req.setPassword("P@ssw0rd1");
        req.setDisplayName("テスト");

        // Act
        UserResponse res = authService.register(req);

        // Assert
        assertNotNull(res);
        assertEquals("user1", res.getUsername());
        assertEquals("test@ginga.info", res.getEmail());
        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCap.capture());
        User savedArg = userCap.getValue();
        assertEquals("hashedpwd", savedArg.getPasswordHash());
        assertFalse(savedArg.isEnabled());
        verify(tokenService, times(1)).createTokenForUser(any(User.class));
        // MailService called asynchronously; still verify invocation started
        verify(mailService, times(1)).send(eq("test@ginga.info"), anyString(), anyString());
    }

    @Test
    void register_duplicateUsername_throws() {
        // ユーザー名が既に存在する場合、DuplicateResourceException が投げられ保存が行われないことを検証する
        // Arrange
        User existing = new User();
        existing.setId(1L);
        when(userRepository.findByUsername("dupuser")).thenReturn(Optional.of(existing));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("dupuser");
        req.setEmail("unique@ginga.info");
        req.setPassword("P@ssw0rd1");

        // Act / Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        // メールアドレスが既に存在する場合、DuplicateResourceException が投げられ保存が行われないことを検証する
        // Arrange
        User existing = new User();
        existing.setId(2L);
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("exists@ginga.info")).thenReturn(Optional.of(existing));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("exists@ginga.info");
        req.setPassword("P@ssw0rd1");

        // Act / Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        // ユーザー名とパスワードが一致する場合、トークンが返されることを検証する
        // Arrange
        String rawPwd = "P@ssw0rd1";
        String encodedPwd = "encodedPwd";
        User user = new User();
        user.setId(1L);
        user.setUsername("taro");
        user.setEmail("taro@example.com");
        user.setPasswordHash(encodedPwd);
        user.setEnabled(true);
        user.setDisplayName("Taro");
        user.setCreatedAt(Instant.now());

        when(userRepository.findByUsername("taro")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPwd, encodedPwd)).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("taro");
        req.setPassword(rawPwd);

        // Act
        LoginResponse res = authService.login(req);

        // Assert
        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("taro", res.getUser().getUsername());
        verify(passwordEncoder).matches(rawPwd, encodedPwd);
    }
    
    @Test
    void login_byEmail_success() {
         // メールアドレスとパスワードが一致する場合、ログインが成功することを検証する
         // Arrange
        String rawPwd = "P@ssw0rd1";
        String encodedPwd = "encodedPwd";
        User user = new User();
        user.setId(1L);
        user.setUsername("taro");
        user.setEmail("taro@example.com");
        user.setPasswordHash(encodedPwd);
        user.setEnabled(true);

        when(userRepository.findByUsername("taro@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPwd, encodedPwd)).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("taro@example.com");
        req.setPassword(rawPwd);

        // Act
        LoginResponse res = authService.login(req);

        // Assert
        assertNotNull(res);
        assertEquals("taro@example.com", res.getUser().getEmail());
    }

    @Test
    void login_invalidCredentials_throws() {
        // 存在しないユーザーの場合、InvalidCredentialsException が投げられることを検証する
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("unknown");
        req.setPassword("pwd");
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }
    
    @Test
    void login_wrongPassword_throws() {
        // パスワードが誤っている場合、InvalidCredentialsException が投げられることを検証する
        // Arrange
        User user = new User();
        user.setUsername("taro");
        user.setPasswordHash("correctHash");
        
        when(userRepository.findByUsername("taro")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "correctHash")).thenReturn(false);
        
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("taro");
        req.setPassword("wrong");
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_notEnabled_throws() {
        // アカウントがまだ有効化されていない場合、AccountNotEnabledException が投げられることを検証する
        // Arrange
        User user = new User();
        user.setUsername("taro");
        user.setPasswordHash("hash");
        user.setEnabled(false);

        when(userRepository.findByUsername("taro")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("taro");
        req.setPassword("pwd");

        // Act & Assert
        assertThrows(AccountNotEnabledException.class, () -> authService.login(req));
    }

    @Test
    void refreshTokens_validToken_returnsNewTokens() {
        // 有効なリフレッシュトークンで新しいアクセストークンとリフレッシュトークンが返され、旧トークンが無効化されることを検証する
        // Arrange
        String refreshTokenValue = "valid-refresh-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        refreshToken.setJti("test-jti");

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TokenResponse response = authService.refreshTokens(refreshTokenValue);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(3600L, response.getExpiresIn());

        // Verify old token was revoked
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        assertTrue(refreshToken.isRevoked());
        assertNotNull(refreshToken.getRevokedAt());
    }

    @Test
    void refreshTokens_invalidToken_throwsException() {
        // 無効なリフレッシュトークン（DBに存在しない）で InvalidTokenException が投げられることを検証する
        // Arrange
        String invalidToken = "invalid-token";
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refreshTokens(invalidToken));
    }

    @Test
    void refreshTokens_revokedToken_throwsException() {
        // 既に無効化されたリフレッシュトークンで InvalidTokenException が投げられることを検証する
        // Arrange
        String revokedTokenValue = "revoked-token";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> authService.refreshTokens(revokedTokenValue));
    }

    @Test
    void refreshTokens_expiredToken_throwsException() {
        // 期限切れのリフレッシュトークンで TokenExpiredException が投げられることを検証する
        // Arrange
        String expiredTokenValue = "expired-token";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired 1 hour ago

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(TokenExpiredException.class, () -> authService.refreshTokens(expiredTokenValue));
    }

    @Test
    void refreshTokens_disabledUser_throwsException() {
        // リフレッシュトークンは有効だがユーザーが無効化されている場合、AccountNotEnabledException が投げられることを検証する
        // Arrange
        String tokenValue = "valid-token";
        User user = new User();
        user.setId(1L);
        user.setEnabled(false); // User is disabled

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(AccountNotEnabledException.class, () -> authService.refreshTokens(tokenValue));
    }

    @Test
    void refreshTokens_updatesLastUsedAt() {
        // リフレッシュトークン使用時に lastUsedAt フィールドが更新されることを検証する
        // Arrange
        String refreshTokenValue = "valid-refresh-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEnabled(true);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        refreshToken.setJti("test-jti");
        refreshToken.setLastUsedAt(null); // Initially null

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        authService.refreshTokens(refreshTokenValue);

        // Assert
        assertNotNull(refreshToken.getLastUsedAt(), "lastUsedAt should be updated");
    }

    @Test
    void refreshTokens_oldTokenCannotBeReusedAfterRotation() {
        // トークンローテーション後、旧トークンが再利用できないことを検証する
        // Arrange
        String oldRefreshTokenValue = "old-refresh-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEnabled(true);

        RefreshToken oldRefreshToken = new RefreshToken();
        oldRefreshToken.setId(1L);
        oldRefreshToken.setUser(user);
        oldRefreshToken.setRevoked(false);
        oldRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        oldRefreshToken.setJti("old-jti");

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act - First use (should succeed)
        TokenResponse firstResponse = authService.refreshTokens(oldRefreshTokenValue);
        assertNotNull(firstResponse);

        // Assert - Old token is now revoked
        assertTrue(oldRefreshToken.isRevoked(), "Old token should be revoked after rotation");
        assertNotNull(oldRefreshToken.getRevokedAt(), "Revoked timestamp should be set");
        assertNotNull(oldRefreshToken.getReplacedBy(), "Should have reference to new token");

        // Act & Assert - Second use should fail
        assertThrows(InvalidTokenException.class, () -> authService.refreshTokens(oldRefreshTokenValue),
            "Old token should not be reusable after rotation");
    }

    @Test
    void login_generatesRefreshToken() {
        // ログイン時にリフレッシュトークンが生成され、DBに保存されることを検証する
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@ginga.info");
        user.setDisplayName("Test User");
        user.setPasswordHash("hashed-password");
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("testuser");
        req.setPassword("password");

        // Act
        LoginResponse response = authService.login(req);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getRefreshToken(), "Refresh token should be generated");
        assertFalse(response.getRefreshToken().isEmpty(), "Refresh token should not be empty");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    // ========== ログアウト関連のテスト ==========

    @Test
    void logout_withValidRefreshToken_revokesToken() {
        // 有効なリフレッシュトークンを指定してログアウトすると、該当トークンが無効化されることを検証する
        // Arrange
        String refreshTokenValue = "valid-refresh-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        authService.logout("testuser", Optional.of(refreshTokenValue), Optional.empty());

        // Assert
        assertTrue(refreshToken.isRevoked(), "Refresh token should be revoked");
        assertNotNull(refreshToken.getRevokedAt(), "Revoked timestamp should be set");
        verify(refreshTokenRepository, times(1)).save(refreshToken);
    }

    @Test
    void logout_withoutRefreshToken_succeedsQuietly() {
        // リフレッシュトークンなしでログアウトしても、エラーなく成功すること（冪等性）を検証する
        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> authService.logout("testuser", Optional.empty(), Optional.empty()));
        
        // Verify no token repository interactions
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logout_withEmptyRefreshToken_succeedsQuietly() {
        // 空のリフレッシュトークン文字列でログアウトしても、エラーなく成功すること（冪等性）を検証する
        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> authService.logout("testuser", Optional.of(""), Optional.empty()));
        
        // Verify no token repository interactions
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logout_withNonExistentToken_succeedsQuietly() {
        // 存在しないトークンでログアウトしても、エラーなく成功すること（冪等性）を検証する
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> authService.logout("testuser", Optional.of("non-existent-token"), Optional.empty()));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logout_withAlreadyRevokedToken_succeedsQuietly() {
        // 既に無効化されたトークンでログアウトしても、エラーなく成功すること（冪等性）を検証する
        // Arrange
        String refreshTokenValue = "already-revoked-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now().minusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> authService.logout("testuser", Optional.of(refreshTokenValue), Optional.empty()));
        
        // Verify no save was called since token was already revoked
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logout_withExpiredToken_succeedsQuietly() {
        // 期限切れのトークンでログアウトしても、エラーなく成功すること（冪等性）を検証する
        // Arrange
        String refreshTokenValue = "expired-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().minusSeconds(3600)); // 1時間前に期限切れ

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> authService.logout("testuser", Optional.of(refreshTokenValue), Optional.empty()));
        
        // 期限切れでも無効化処理は実行される
        assertTrue(refreshToken.isRevoked(), "Expired token should still be revoked");
        verify(refreshTokenRepository, times(1)).save(refreshToken);
    }

    @Test
    void logout_withTokenBelongingToOtherUser_throwsException() {
        // 他のユーザーのトークンでログアウトを試みた場合、InvalidTokenException が投げられることを検証する
        // Arrange
        String refreshTokenValue = "other-users-token";
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(otherUser);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThrows(InvalidTokenException.class, 
            () -> authService.logout("testuser", Optional.of(refreshTokenValue), Optional.empty()),
            "Should throw InvalidTokenException when token belongs to another user");
        
        // Verify token was not revoked
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // ========== Redis ブラックリスト関連のテスト ==========

    @Test
    void logout_withJti_addsToBlacklist() {
        // jti が指定されている場合、Redis ブラックリストに追加されることを検証する
        // Arrange
        String jti = "test-jti-12345";

        // Act
        authService.logout("testuser", Optional.empty(), Optional.of(jti));

        // Assert
        verify(tokenBlacklistService, times(1)).addToBlacklist(eq(jti), eq(3600L));
    }

    @Test
    void logout_withEmptyJti_doesNotAddToBlacklist() {
        // 空の jti の場合、Redis ブラックリストには追加されないことを検証する
        // Act
        authService.logout("testuser", Optional.empty(), Optional.of(""));

        // Assert
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyLong());
    }

    @Test
    void logout_withoutJti_doesNotAddToBlacklist() {
        // jti が指定されていない場合、Redis ブラックリストには追加されないことを検証する
        // Act
        authService.logout("testuser", Optional.empty(), Optional.empty());

        // Assert
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyLong());
    }

    @Test
    void logout_withRefreshTokenAndJti_revokesBothTokens() {
        // リフレッシュトークンと jti の両方が指定されている場合、両方が無効化されることを検証する
        // Arrange
        String refreshTokenValue = "valid-refresh-token";
        String jti = "test-jti-12345";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        authService.logout("testuser", Optional.of(refreshTokenValue), Optional.of(jti));

        // Assert
        assertTrue(refreshToken.isRevoked(), "Refresh token should be revoked");
        verify(refreshTokenRepository, times(1)).save(refreshToken);
        verify(tokenBlacklistService, times(1)).addToBlacklist(eq(jti), eq(3600L));
    }
}

