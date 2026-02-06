package com.ginga.naviai.auth.filter;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

        // jti が指定されている場合、ブラックリストをチェック
        if (jti != null && !jti.isEmpty()) {
            if (tokenBlacklistService.isBlacklisted(jti)) {
                logger.warn("Token with jti {} is blacklisted", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token has been revoked\"}");
                return;
            }
        }

        // 将来の JWT 実装: ここでトークンを検証し、クレームから jti を取得して
        // ブラックリストチェックを行う
        // String token = authHeader.substring(BEARER_PREFIX.length());
        // Claims claims = validateAndParseJwt(token);
        // String jtiFromToken = claims.get("jti", String.class);
        // if (tokenBlacklistService.isBlacklisted(jtiFromToken)) { ... }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 認証不要のエンドポイントをスキップ
        return path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/confirm") ||
               path.startsWith("/h2-console");
    }
}
