package com.ginga.naviai.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

public class JwtTokenUtil {

    private static Key getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateAccessToken(String subject, String jti, long expirationSeconds, String secret) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
            .setSubject(subject)
            .setId(jti)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
            .compact();
    }

    public static Claims parseClaims(String token, String secret) {
        Jws<Claims> jws = Jwts.parserBuilder()
            .setSigningKey(getSigningKey(secret))
            .build()
            .parseClaimsJws(token);
        return jws.getBody();
    }
}
