package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.LoginRequest;
import com.ginga.naviai.auth.dto.LoginResponse;
import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.exception.AccountNotEnabledException;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.exception.InvalidCredentialsException;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ConfirmationTokenService tokenService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
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
    void lo存在しないユーザーの場合、InvalidCredentialsException が投げられることを検証する
        // gin_invalidCredentials_throws() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("unknown");
        req.setPassword("pwd");
        
        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }
    
    @Testパスワードが誤っている場合、InvalidCredentialsException が投げられることを検証する
        // 
    void login_wrongPassword_throws() {
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
アカウントがまだ有効化されていない場合、AccountNotEnabledException が投げられることを検証する
        // 
    @Test
    void login_notEnabled_throws() {
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
}
