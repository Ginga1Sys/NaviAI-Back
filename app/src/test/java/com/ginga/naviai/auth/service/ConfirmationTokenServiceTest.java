package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.entity.ConfirmationToken;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.ConfirmationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfirmationTokenServiceTest {

    @Mock
    private ConfirmationTokenRepository tokenRepository;

    @InjectMocks
    private ConfirmationTokenService tokenService;

    @Test
    void createTokenForUser_savesTokenAndReturnsString() {
        // トークン作成がユーザーに紐づく ConfirmationToken を永続化し、トークン文字列を返すことを検証する
        User u = new User();
        u.setId(5L);
        ArgumentCaptor<ConfirmationToken> cap = ArgumentCaptor.forClass(ConfirmationToken.class);
        when(tokenRepository.save(any(ConfirmationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = tokenService.createTokenForUser(u);
        assertNotNull(token);
        verify(tokenRepository, times(1)).save(cap.capture());
        ConfirmationToken saved = cap.getValue();
        assertEquals(u, saved.getUser());
        assertNotNull(saved.getExpiresAt());
    }

    @Test
    void findByToken_delegatesToRepository() {
        // findByToken がリポジトリに委譲し、トークンが存在する場合に正しい値を返すことを検証する
        ConfirmationToken ct = new ConfirmationToken();
        ct.setToken("t1");
        when(tokenRepository.findByToken("t1")).thenReturn(Optional.of(ct));
        Optional<ConfirmationToken> res = tokenService.findByToken("t1");
        assertTrue(res.isPresent());
        assertEquals("t1", res.get().getToken());
    }

    @Test
    void setConfirmedAt_updatesAndSaves() {
        // setConfirmedAt が ConfirmationToken.confirmedAt を更新し、リポジトリに保存することを検証する
        ConfirmationToken ct = new ConfirmationToken();
        ct.setToken("t2");
        tokenService.setConfirmedAt(ct, Instant.now());
        verify(tokenRepository, times(1)).save(ct);
        assertNotNull(ct.getConfirmedAt());
    }
}
