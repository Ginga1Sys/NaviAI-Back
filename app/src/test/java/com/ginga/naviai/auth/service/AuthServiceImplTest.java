package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
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
}
