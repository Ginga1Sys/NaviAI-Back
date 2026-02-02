package com.ginga.naviai.auth.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenUtilTest {

    @Test
    void generateSecureToken_returnsNonNullToken() {
        // トークン生成が null でないことを検証
        String token = TokenUtil.generateSecureToken();
        assertNotNull(token);
    }

    @Test
    void generateSecureToken_returnsUniqueTokens() {
        // 生成されるトークンがユニークであることを検証
        String token1 = TokenUtil.generateSecureToken();
        String token2 = TokenUtil.generateSecureToken();
        assertNotEquals(token1, token2);
    }

    @Test
    void generateSecureToken_returnsTokenWithExpectedLength() {
        // Base64エンコード後のトークンが適切な長さを持つことを検証（32バイト -> 約43文字）
        String token = TokenUtil.generateSecureToken();
        assertTrue(token.length() >= 40, "Token should be at least 40 characters");
    }

    @Test
    void hashToken_returnsSameHashForSameInput() {
        // 同じ入力に対して常に同じハッシュが返されることを検証
        String token = "test-token";
        String secret = "test-secret";
        
        String hash1 = TokenUtil.hashToken(token, secret);
        String hash2 = TokenUtil.hashToken(token, secret);
        
        assertEquals(hash1, hash2);
    }

    @Test
    void hashToken_returnsDifferentHashForDifferentToken() {
        // 異なるトークンに対して異なるハッシュが返されることを検証
        String secret = "test-secret";
        
        String hash1 = TokenUtil.hashToken("token1", secret);
        String hash2 = TokenUtil.hashToken("token2", secret);
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashToken_returnsDifferentHashForDifferentSecret() {
        // 異なる秘密鍵に対して異なるハッシュが返されることを検証
        String token = "test-token";
        
        String hash1 = TokenUtil.hashToken(token, "secret1");
        String hash2 = TokenUtil.hashToken(token, "secret2");
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashToken_returnsHexEncodedString() {
        // ハッシュが16進数文字列であることを検証（64文字 = 32バイト）
        String token = "test-token";
        String secret = "test-secret";
        
        String hash = TokenUtil.hashToken(token, secret);
        
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
        assertTrue(hash.matches("^[0-9a-f]+$"), "Hash should only contain hex characters");
    }
}
