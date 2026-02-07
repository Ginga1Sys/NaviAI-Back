package com.ginga.naviai.auth.filter;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import com.ginga.naviai.auth.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import java.io.IOException;

/**
 * JWT 認証フィルタ
 * アクセストークンの jti がブラックリストに登録されていないか検証する
 * 
 * 注意: 現在の実装ではアクセストークンは UUID 形式であり、JWT 形式ではない。
 * 将来 JWT に移行する際は、トークンからクレームを抽出して jti を取得するロジックを追加する。
 * 現時点では、クライアントがリクエストヘッダで jti を渡す設計を想定。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JTI_HEADER = "X-Token-Jti";

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${token.secret}")
    private String tokenSecret;

    @Autowired
    public JwtAuthenticationFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String jti = request.getHeader(JTI_HEADER);
        
        // Authorization ヘッダがない場合はスキップ（認証不要のエンドポイント）
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenValue = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (tokenValue.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = JwtTokenUtil.parseClaims(tokenValue, tokenSecret);
            String jtiFromToken = claims.getId();
            String subject = claims.getSubject();

            String blacklistJti = (jtiFromToken != null && !jtiFromToken.isEmpty()) ? jtiFromToken : jti;
            if (blacklistJti != null && !blacklistJti.isEmpty()) {
                if (tokenBlacklistService.isBlacklisted(blacklistJti)) {
                    logger.warn("Token with jti {} is blacklisted", blacklistJti);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Token has been revoked\"}");
                    return;
                }
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException ex) {
            logger.warn("Invalid access token", ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid access token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 認証不要のエンドポイントをスキップ
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/h2-console");
    }
}
