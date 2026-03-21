package com.ginga.naviai.tags.controller;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import com.ginga.naviai.tags.dto.TagResponse;
import com.ginga.naviai.tags.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TagService tagService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    /**
     * タグ一覧 API が 200 で配列を返すことを確認する。
     */
    @Test
    @WithMockUser
    void getAllTags_returns200WithTagList() throws Exception {
        when(tagService.getAllTags()).thenReturn(List.of(
                TagResponse.builder().name("機械学習").count(42L).build(),
                TagResponse.builder().name("設計").count(12L).build()
        ));

        mockMvc.perform(get("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("機械学習"))
                .andExpect(jsonPath("$[0].count").value(42))
                .andExpect(jsonPath("$[1].name").value("設計"))
                .andExpect(jsonPath("$[1].count").value(12));
    }

    /**
     * 未認証アクセス時に 401 が返ることを確認する。
     */
    @Test
    void getAllTags_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * サービス層で例外が発生した場合に 500 が返ることを確認する。
     */
    @Test
    @WithMockUser
    void getAllTags_whenServiceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("unexpected error")).when(tagService).getAllTags();

        mockMvc.perform(get("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
