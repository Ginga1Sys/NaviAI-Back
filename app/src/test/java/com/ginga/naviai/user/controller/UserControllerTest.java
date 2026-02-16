package com.ginga.naviai.user.controller;

import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.config.SecurityConfig;
import com.ginga.naviai.user.exception.UserNotFoundException;
import com.ginga.naviai.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private com.ginga.naviai.auth.filter.JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setup() throws Exception {
        // Make the mocked filter pass the request through so controller is reached
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void getCurrentUser_unauthorized_returns401() throws Exception {
        // no authentication attached to request
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void getCurrentUser_success_returns200() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("1", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserResponse res = new UserResponse();
        res.setId(1L);
        res.setUsername("testuser");
        res.setEmail("test@ginga.info");
        res.setDisplayName("Test User");
        res.setCreatedAt(java.time.Instant.parse("2026-02-07T10:20:30Z"));

        when(userService.getCurrentUser(1L)).thenReturn(res);
        try {
            mockMvc.perform(get("/api/v1/users/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.createdAt").value("2026-02-07T10:20:30Z"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @WithMockUser(username = "99")
    void getCurrentUser_notFound_returns404() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("99", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        doThrow(new UserNotFoundException("User not found"))
            .when(userService).getCurrentUser(99L);

        try {
            mockMvc.perform(get("/api/v1/users/me").with(authentication(auth)))
                .andExpect(status().isNotFound());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @WithMockUser(username = "invalid")
    void getCurrentUser_invalidSubject_returns401() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("invalid", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        try {
            mockMvc.perform(get("/api/v1/users/me").with(authentication(auth)))
                .andExpect(status().isUnauthorized());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @WithMockUser(username = " ")
    void getCurrentUser_blankSubject_returns401() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(" ", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        try {
            mockMvc.perform(get("/api/v1/users/me").with(authentication(auth)))
                .andExpect(status().isUnauthorized());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @WithMockUser(username = "1")
    void getCurrentUser_serviceError_returns500() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("1", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        doThrow(new RuntimeException("unexpected"))
            .when(userService).getCurrentUser(1L);

        try {
            mockMvc.perform(get("/api/v1/users/me").with(authentication(auth)))
                .andExpect(status().isInternalServerError());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
