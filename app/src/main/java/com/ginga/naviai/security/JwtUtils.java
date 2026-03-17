package com.ginga.naviai.security;

import com.ginga.naviai.auth.exception.InvalidTokenException;
import com.ginga.naviai.auth.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret:change-this-secret-to-a-long-random-value}")
    private String jwtSecret;

    private Key key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or malformed token");
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or malformed token");
        }
    }

    @SuppressWarnings("unchecked")
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        Claims claims = getClaims(token);
        String subject = claims.getSubject();

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            ((List<Object>) rolesObj).forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString())));
        } else if (rolesObj instanceof String) {
            String[] parts = rolesObj.toString().split(",");
            for (String p : parts) authorities.add(new SimpleGrantedAuthority("ROLE_" + p.trim()));
        }

        Object permsObj = claims.get("permissions");
        if (permsObj instanceof List) {
            ((List<Object>) permsObj).forEach(p -> authorities.add(new SimpleGrantedAuthority(p.toString())));
        } else if (permsObj instanceof String) {
            String[] parts = permsObj.toString().split(",");
            for (String p : parts) authorities.add(new SimpleGrantedAuthority(p.trim()));
        }

        return new UsernamePasswordAuthenticationToken(subject, token, authorities);
    }
}
