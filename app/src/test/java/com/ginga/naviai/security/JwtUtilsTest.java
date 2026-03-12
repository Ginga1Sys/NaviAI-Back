package com.ginga.naviai.security;

import com.ginga.naviai.auth.exception.TokenExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.List;

public class JwtUtilsTest {

    @AfterEach
    public void cleanup() {
        // clear if needed
    }

    @Test
    public void validTokenIsAccepted() {
        JwtUtils utils = new JwtUtils();
        String secret = "test-secret-which-is-long-enough-to-be-used-by-jjwt-0123456789";
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        utils.init();

        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .setSubject("alice")
                .claim("roles", List.of("USER"))
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        Assertions.assertTrue(utils.validateToken(token));
    }

    @Test
    public void expiredTokenThrows() {
        JwtUtils utils = new JwtUtils();
        String secret = "test-secret-which-is-long-enough-to-be-used-by-jjwt-0123456789";
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        utils.init();

        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        Assertions.assertThrows(TokenExpiredException.class, () -> utils.validateToken(token));
    }
}
