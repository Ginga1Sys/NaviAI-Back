package com.ginga.naviai.knowledge.controller;

import org.springframework.security.core.userdetails.User;
import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO:テストを実行すると失敗するので修正する
@WebMvcTest(KnowledgeController.class)
public class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @Test
    @WithMockUser
    public void testGetMyKnowledge() throws Exception {
        User userDetails = new User("user", "password", Collections.emptyList());
        Page<KnowledgeResponse> page = new PageImpl<>(Collections.emptyList());
        when(knowledgeService.getMyKnowledgeByUsername(eq("user"), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/knowledge?mine=true")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    public void testGetKnowledgeByAuthorId() throws Exception {
        Page<KnowledgeResponse> page = new PageImpl<>(Collections.emptyList());
        when(knowledgeService.getKnowledgeByAuthorId(eq(1L), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/knowledge?author_id=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    public void testGetKnowledge_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge?mine=true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetKnowledge_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge"))
                .andExpect(status().isBadRequest());
    }
}
