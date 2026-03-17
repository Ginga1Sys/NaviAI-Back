package com.ginga.naviai.security;

import com.ginga.naviai.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Key;
import java.util.Date;
import java.util.List;

public class JwtAuthFilterTest {

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void noHeader_allowsThroughWithoutAuthentication() throws Exception {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", "test-secret-which-is-long-enough-to-be-used-by-jjwt-0123456789");
        utils.init();

        JwtAuthFilter filter = new JwtAuthFilter(utils);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void invalidToken_returns401() throws Exception {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", "test-secret-which-is-long-enough-to-be-used-by-jjwt-0123456789");
        utils.init();

        JwtAuthFilter filter = new JwtAuthFilter(utils);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        Assertions.assertEquals(401, res.getStatus());
    }

    @Test
    public void validToken_setsAuthentication() throws Exception {
        JwtUtils utils = new JwtUtils();
        String secret = "test-secret-which-is-long-enough-to-be-used-by-jjwt-0123456789";
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        utils.init();

        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        String token = Jwts.builder()
                .setSubject("bob")
                .claim("roles", List.of("USER"))
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        JwtAuthFilter filter = new JwtAuthFilter(utils);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertEquals("bob", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
