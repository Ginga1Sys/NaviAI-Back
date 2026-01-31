package com.ginga.naviai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.exception.DuplicateResourceException;
import com.ginga.naviai.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private com.ginga.naviai.auth.service.ConfirmationTokenService confirmationTokenService;
    @MockBean
    private com.ginga.naviai.auth.repository.UserRepository userRepository;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void register_controller_success_returns201() throws Exception {
        // 有効な登録リクエストが HTTP 201 を返し、作成されたユーザーのフィールドがレスポンスに含まれることを検証する
        // Arrange: mock service to return response
        UserResponse ur = new UserResponse();
        ur.setId(10L);
        ur.setUsername("ctrlUser");
        ur.setEmail("ctrl@ginga.info");
        when(authService.register(any(RegisterRequest.class))).thenReturn(ur);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("ctrlUser");
        req.setEmail("ctrl@ginga.info");
        req.setPassword("Aa1!aaaa");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.username").value("ctrlUser"));
    }

    @Test
    void register_controller_validationError_returns400() throws Exception {
        // 無効なリクエスト（バリデーション失敗）が HTTP 400 を返すことを検証する
        // Arrange: invalid email domain
        RegisterRequest req = new RegisterRequest();
        req.setUsername("x");
        req.setEmail("bad@other.com");
        req.setPassword("weak");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_controller_duplicate_returns409() throws Exception {
        // サービスが DuplicateResourceException を投げたとき、コントローラが HTTP 409 を返すことを検証する
        // Arrange: service throws DuplicateResourceException
        doThrow(new DuplicateResourceException("email exists")).when(authService).register(any(RegisterRequest.class));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("dup");
        req.setEmail("dup@ginga.info");
        req.setPassword("Aa1!aaaa");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @org.junit.jupiter.api.Test
    void confirm_endpoint_invalidToken_returns400() throws Exception {
        // confirm エンドポイントが不正／存在しないトークンに対して HTTP 400 を返すことを検証する
        when(confirmationTokenService.findByToken("bad")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/confirm")
                .param("token", "bad"))
            .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void confirm_endpoint_validToken_enablesUser() throws Exception {
        // 有効なトークンが関連ユーザーを有効化し、HTTP 200 を返すことを検証する
        com.ginga.naviai.auth.entity.ConfirmationToken ct = new com.ginga.naviai.auth.entity.ConfirmationToken();
        com.ginga.naviai.auth.entity.User u = new com.ginga.naviai.auth.entity.User();
        u.setId(50L);
        u.setEnabled(false);
        ct.setUser(u);
        ct.setToken("t1");
        ct.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        when(confirmationTokenService.findByToken("t1")).thenReturn(java.util.Optional.of(ct));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/confirm")
                .param("token", "t1"))
            .andExpect(status().isOk());
        // userRepository.save called via controller
        verify(userRepository, times(1)).save(any());
    }

    @org.junit.jupiter.api.Test
    void confirm_endpoint_expiredToken_returns400() throws Exception {
        // 期限切れトークンが HTTP 400 を返すこと（トークン期限切れ）を検証する
        com.ginga.naviai.auth.entity.ConfirmationToken ct = new com.ginga.naviai.auth.entity.ConfirmationToken();
        ct.setToken("t-exp");
        com.ginga.naviai.auth.entity.User u = new com.ginga.naviai.auth.entity.User();
        u.setId(51L);
        ct.setUser(u);
        ct.setExpiresAt(java.time.Instant.now().minusSeconds(3600));
        when(confirmationTokenService.findByToken("t-exp")).thenReturn(java.util.Optional.of(ct));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/confirm")
                .param("token", "t-exp"))
            .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void confirm_endpoint_alreadyConfirmed_returnsOk() throws Exception {
        // 既に確認済みのトークンが呼ばれた場合、冪等的に HTTP 200 を返すことを検証する
        com.ginga.naviai.auth.entity.ConfirmationToken ct = new com.ginga.naviai.auth.entity.ConfirmationToken();
        ct.setToken("t-already");
        com.ginga.naviai.auth.entity.User u = new com.ginga.naviai.auth.entity.User();
        u.setId(52L);
        u.setEnabled(false);
        ct.setUser(u);
        ct.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        ct.setConfirmedAt(java.time.Instant.now().minusSeconds(10));
        when(confirmationTokenService.findByToken("t-already")).thenReturn(java.util.Optional.of(ct));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/confirm")
                .param("token", "t-already"))
            .andExpect(status().isOk());
    }
}
