<<<<<<< HEAD
package com.ginga.naviai.knowledge.controller;

import com.ginga.naviai.auth.service.TokenBlacklistService;
import com.ginga.naviai.knowledge.dto.KnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KnowledgeController の単体テスト（MockMvc）。
 * KnowledgeService をモックし、エンドポイントのパラメータ受取・レスポンス形状・認証を検証する。
 */
@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    // ── ユーティリティ ─────────────────────────────────────────────

    /** 空ページレスポンスのスタブを設定する。 */
    private void stubEmptyResponse() {
        KnowledgePageResponse empty = KnowledgePageResponse.builder()
                .page(0).size(20).totalElements(0L).items(Collections.emptyList()).build();
        when(knowledgeService.search(any())).thenReturn(empty);
    }

    // ── テスト ─────────────────────────────────────────────────────

    /**
     * 【正常系】パラメータなしでも 200 OK が返り、
     * レスポンスJSONがページングスキーマ（page, size, totalElements, items）を持つこと。
     */
    @Test
    @WithMockUser
    void search_withNoParams_returns200WithSchema() throws Exception {
        stubEmptyResponse();

        mockMvc.perform(get("/api/v1/knowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items").isArray());
    }

    /**
     * 【正常系】全パラメータ（q, sort, filter, page, size, tags）を指定して 200 OK が返ること。
     * レスポンスの items に正しいフィールドが含まれること。
     */
    @Test
    @WithMockUser
    void search_withAllParams_returns200WithItems() throws Exception {
        KnowledgeItemDto item = KnowledgeItemDto.builder()
                .id(1L)
                .title("記事タイトル")
                .summary("要約テキスト")
                .author("user01")
                .createdAt(Instant.parse("2026-02-01T00:00:00Z"))
                .score(10L)
                .tags(List.of("AI", "設計"))
                .build();

        KnowledgePageResponse response = KnowledgePageResponse.builder()
                .page(0).size(5).totalElements(1L).items(List.of(item)).build();

        when(knowledgeService.search(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/knowledge")
                        .param("q", "AI")
                        .param("sort", "score")
                        .param("filter", "recommended")
                        .param("page", "0")
                        .param("size", "5")
                        .param("tags", "AI,設計")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("記事タイトル"))
                .andExpect(jsonPath("$.items[0].summary").value("要約テキスト"))
                .andExpect(jsonPath("$.items[0].author").value("user01"))
                .andExpect(jsonPath("$.items[0].score").value(10))
                .andExpect(jsonPath("$.items[0].tags[0]").value("AI"));
    }

    /**
     * 【異常系】page=-1 のような不正パラメータを送信した場合、400 Bad Request が返ること。
     * バリデーション（@Min(0)）が適用されることを確認する。
     */
    @Test
    @WithMockUser
    void search_withNegativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * 【異常系】size=0 のような不正パラメータを送信した場合、400 Bad Request が返ること。
     * バリデーション（@Min(1)）が適用されることを確認する。
     */
    @Test
    @WithMockUser
    void search_withZeroSize_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * 【異常系】size > 100 の場合、400 Bad Request が返ること。
     * バリデーション（@Max(100)）が適用されることを確認する。
     */
    @Test
    @WithMockUser
    void search_withOversizedPage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * 【異常系】未ログイン（認証なし）でアクセスした場合、
     * 401 Unauthorized が返ること（Spring Security による制御）。
     */
    @Test
    void search_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 【正常系】filter=recommended を指定して認証済みでアクセスした場合、
     * 200 OK が返ること。
     */
    @Test
    @WithMockUser
    void search_filterRecommended_withAuth_returns200() throws Exception {
        stubEmptyResponse();

        mockMvc.perform(get("/api/v1/knowledge")
                        .param("filter", "recommended")
                        .param("sort", "created_at")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
=======
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
>>>>>>> 4d0036690621c93b680f4a1863ad2e8a4f3d0c24
