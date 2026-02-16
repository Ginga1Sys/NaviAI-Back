package com.ginga.naviai.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setup() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    // ========== addToBlacklist テスト ==========

    @Test
    void addToBlacklist_validJti_addsToRedis() {
        // 有効な jti が Redis に正しく登録されることを検証する
        // Arrange
        String jti = "test-jti-12345";
        long ttlSeconds = 3600L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        tokenBlacklistService.addToBlacklist(jti, ttlSeconds);

        // Assert
        verify(valueOperations, times(1)).set(
            eq("auth:blacklist:" + jti),
            eq("revoked"),
            eq(ttlSeconds),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void addToBlacklist_nullJti_doesNothing() {
        // null の jti では何も処理されないことを検証する
        // Act
        tokenBlacklistService.addToBlacklist(null, 3600L);

        // Assert
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void addToBlacklist_emptyJti_doesNothing() {
        // 空の jti では何も処理されないことを検証する
        // Act
        tokenBlacklistService.addToBlacklist("", 3600L);

        // Assert
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void addToBlacklist_redisError_handledGracefully() {
        // Redis エラーが発生しても例外がスローされないことを検証する
        // Arrange
        String jti = "test-jti-12345";
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(jti, 3600L));
    }

    // ========== isBlacklisted テスト ==========

    @Test
    void isBlacklisted_existingKey_returnsTrue() {
        // ブラックリストに存在するキーに対して true を返すことを検証する
        // Arrange
        String jti = "blacklisted-jti";
        when(redisTemplate.hasKey("auth:blacklist:" + jti)).thenReturn(true);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(jti);

        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey("auth:blacklist:" + jti);
    }

    @Test
    void isBlacklisted_nonExistingKey_returnsFalse() {
        // ブラックリストに存在しないキーに対して false を返すことを検証する
        // Arrange
        String jti = "valid-jti";
        when(redisTemplate.hasKey("auth:blacklist:" + jti)).thenReturn(false);

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(jti);

        // Assert
        assertFalse(result);
    }

    @Test
    void isBlacklisted_nullJti_returnsFalse() {
        // null の jti に対して false を返すことを検証する
        // Act
        boolean result = tokenBlacklistService.isBlacklisted(null);

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isBlacklisted_emptyJti_returnsFalse() {
        // 空の jti に対して false を返すことを検証する
        // Act
        boolean result = tokenBlacklistService.isBlacklisted("");

        // Assert
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void isBlacklisted_redisError_returnsFalse() {
        // Redis エラー時に false を返す（フェイルオープン設計）ことを検証する
        // Arrange
        String jti = "test-jti";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean result = tokenBlacklistService.isBlacklisted(jti);

        // Assert
        assertFalse(result);
    }

    // ========== removeFromBlacklist テスト ==========

    @Test
    void removeFromBlacklist_validJti_removesFromRedis() {
        // 有効な jti が Redis から削除されることを検証する
        // Arrange
        String jti = "test-jti-12345";
        when(redisTemplate.delete("auth:blacklist:" + jti)).thenReturn(true);

        // Act
        tokenBlacklistService.removeFromBlacklist(jti);

        // Assert
        verify(redisTemplate, times(1)).delete("auth:blacklist:" + jti);
    }

    @Test
    void removeFromBlacklist_nullJti_doesNothing() {
        // null の jti では何も処理されないことを検証する
        // Act
        tokenBlacklistService.removeFromBlacklist(null);

        // Assert
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void removeFromBlacklist_emptyJti_doesNothing() {
        // 空の jti では何も処理されないことを検証する
        // Act
        tokenBlacklistService.removeFromBlacklist("");

        // Assert
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void removeFromBlacklist_redisError_handledGracefully() {
        // Redis エラーが発生しても例外がスローされないことを検証する
        // Arrange
        String jti = "test-jti";
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> tokenBlacklistService.removeFromBlacklist(jti));
    }
}
