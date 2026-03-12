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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Arrays;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeController.class)
public class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private com.ginga.naviai.auth.service.TokenBlacklistService tokenBlacklistService;

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
    @WithMockUser
    public void testGetMyKnowledge_singleItem() throws Exception {
        KnowledgeResponse item = Mockito.mock(KnowledgeResponse.class);
        Page<KnowledgeResponse> page = new PageImpl<>(Collections.singletonList(item));
        when(knowledgeService.getMyKnowledgeByUsername(eq("user"), any(PageRequest.class))).thenReturn(page);

        User userDetails = new User("user", "password", Collections.emptyList());
        mockMvc.perform(get("/api/v1/knowledge?mine=true").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    @WithMockUser
    public void testGetMyKnowledge_multipleItems() throws Exception {
        KnowledgeResponse a = Mockito.mock(KnowledgeResponse.class);
        KnowledgeResponse b = Mockito.mock(KnowledgeResponse.class);
        Page<KnowledgeResponse> page = new PageImpl<>(Arrays.asList(a, b));
        when(knowledgeService.getMyKnowledgeByUsername(eq("user"), any(PageRequest.class))).thenReturn(page);

        User userDetails = new User("user", "password", Collections.emptyList());
        mockMvc.perform(get("/api/v1/knowledge?mine=true").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(2));
    }

    @Test
    @WithMockUser
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
    @WithMockUser
    public void testGetKnowledge_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge"))
                .andExpect(status().isBadRequest());
    }

}
