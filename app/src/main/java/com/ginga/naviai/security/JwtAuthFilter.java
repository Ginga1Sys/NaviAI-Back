package com.ginga.naviai.security;

import com.ginga.naviai.auth.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * RBAC ヘルパー用 JWT フィルタ（Spring 管理 Bean 外）。
 * <p>
 * このクラスはセキュリティフィルタチェーンには登録しない。
 * 実際の JWT 検証・SecurityContext 設定は JwtAuthenticationFilter が担当する。
 * RbacAspect は SecurityContextHolder を直接参照するため、このクラスを Spring Bean にする必要はない。
 * {@code @Component} を付与しないことで、@WebMvcTest 等のスライドテストで
 * JwtUtils が未解決になる問題を回避する。
 * </p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                jwtUtils.validateToken(token);
                SecurityContextHolder.getContext().setAuthentication(jwtUtils.getAuthentication(token));
            } catch (InvalidTokenException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
