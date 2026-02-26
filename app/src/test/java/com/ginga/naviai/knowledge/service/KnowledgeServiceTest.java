package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeServiceImpl の単体テスト。
 * NamedParameterJdbcTemplate をモックし、各種検索条件・フィルタ・ソートの動作を検証する。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private KnowledgeServiceImpl service;

    // ── ユーティリティ ─────────────────────────────────────────────

    /** COUNT クエリが 0 を返すようにスタブを設定する。 */
    private void stubCountZero() {
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
    }

    /** COUNT クエリが指定件数を返すようにスタブを設定する。 */
    private void stubCount(long count) {
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(count);
    }

    /** データクエリが空リストを返すようにスタブを設定する。 */
    private void stubEmptyDataRows() {
        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(Collections.emptyList());
    }

    /** 1件分のデータ行を生成する。 */
    private Map<String, Object> buildRow(long id, String title, String body) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("title", title);
        row.put("body", body);
        row.put("author", "testuser");
        row.put("created_at", Timestamp.from(Instant.parse("2026-02-01T00:00:00Z")));
        row.put("like_count", 5L);
        return row;
    }

    // ── テスト ─────────────────────────────────────────────────────

    /**
     * 【正常系】パラメータ未指定（デフォルト）で呼び出した場合、
     * 空の結果でも page=0, size=20 を含む正しい構造が返ること。
     */
    @Test
    void search_withDefaultParams_returnsEmptyPage() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        KnowledgePageResponse result = service.search(req);

        // ページング情報が正しく設定されていること
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(0L, result.getTotalElements());
        assertTrue(result.getItems().isEmpty());
    }

    /**
     * 【正常系】検索語 q がある場合、クエリパラメータ "q" が
     * NamedParameterJdbcTemplate に渡されること（部分一致形式 %keyword%）。
     */
    @Test
    void search_withQ_addsQParamToQuery() {
        stubCount(1L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setQ("spring");

        service.search(req);

        // COUNT クエリのパラメータを検証
        ArgumentCaptor<SqlParameterSource> paramCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForObject(anyString(), paramCaptor.capture(), eq(Long.class));

        SqlParameterSource params = paramCaptor.getValue();
        // "q" パラメータが %spring% 形式で設定されていること
        assertEquals("%spring%", params.getValue("q"));
    }

    /**
     * 【正常系】sort=created_at の場合、データクエリの SQL に
     * "k.created_at DESC" が含まれること。
     */
    @Test
    void search_withSortCreatedAt_usesCreatedAtDescOrder() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setSort("created_at");

        service.search(req);

        // データクエリの SQL を検証
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));

        // SQL に ORDER BY k.created_at DESC が含まれること（並び順の検証）
        assertTrue(sqlCaptor.getValue().contains("k.created_at DESC"),
                "sort=created_at のとき ORDER BY k.created_at DESC が使われること");
    }

    /**
     * 【正常系】filter=latest の場合、
     * - ORDER BY k.created_at DESC が使われること
     * - size が 20 を超えていても 20 に制限されること。
     */
    @Test
    void search_filterLatest_capsSizeTo20AndUsesCreatedAtOrder() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setFilter("latest");
        req.setSize(50); // 50 を指定 → 20 に制限されるはず

        KnowledgePageResponse result = service.search(req);

        // size が 20 に制限されていること（filter=latest の最大20件制限）
        assertEquals(20, result.getSize());

        // データクエリが k.created_at DESC を使っていること
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));
        assertTrue(sqlCaptor.getValue().contains("k.created_at DESC"),
                "filter=latest のとき ORDER BY k.created_at DESC が使われること");
    }

    /**
     * 【正常系】filter=recommended の場合、
     * データクエリの SQL に "like_count DESC" が含まれること（いいね数降順）。
     */
    @Test
    void search_filterRecommended_usesLikeCountDescOrder() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setFilter("recommended");

        service.search(req);

        // データクエリの SQL を検証
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));

        // おすすめ定義：いいね数（like_count）が多い順で返すこと
        assertTrue(sqlCaptor.getValue().contains("like_count DESC"),
                "filter=recommended のとき ORDER BY like_count DESC が使われること");
    }

    /**
     * 【正常系】sort パラメータ（score）は filter より優先されること。
     * filter=recommended かつ sort=score → like_count DESC（変わらないが確認）。
     * filter=recommended かつ sort=created_at → k.created_at DESC に上書きされること。
     */
    @Test
    void search_sortOverridesFilter() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setFilter("recommended");
        req.setSort("created_at"); // sort が filter を上書きする

        service.search(req);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));

        // sort=created_at の場合、filter=recommended の like_count DESC より created_at DESC が優先
        assertTrue(sqlCaptor.getValue().contains("k.created_at DESC"),
                "sort パラメータが filter より優先されること");
    }

    /**
     * 【正常系】ページング（page, size）が正しく LIMIT/OFFSET に反映されること。
     */
    @Test
    void search_paginationParametersAreApplied() {
        stubCount(100L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setPage(2);
        req.setSize(10);

        KnowledgePageResponse result = service.search(req);

        // レスポンスに page と size が反映されていること
        assertEquals(2, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(100L, result.getTotalElements());

        // LIMIT/OFFSET パラメータの検証
        ArgumentCaptor<SqlParameterSource> paramCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForList(anyString(), paramCaptor.capture());

        assertEquals(10, paramCaptor.getValue().getValue("size"));
        assertEquals(20L, paramCaptor.getValue().getValue("offset")); // page=2, size=10 → offset=20
    }

    /**
     * 【正常系】タグフィルタ（tags）が指定されている場合、
     * "tagNames" パラメータが設定されること。
     */
    @Test
    void search_withTags_addsTagNamesToParams() {
        stubCount(0L);
        stubEmptyDataRows();

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        req.setTags("AI,設計");

        service.search(req);

        ArgumentCaptor<SqlParameterSource> paramCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForObject(anyString(), paramCaptor.capture(), eq(Long.class));

        // tagNames パラメータが設定されていること
        Object tagNamesParam = paramCaptor.getValue().getValue("tagNames");
        assertNotNull(tagNamesParam, "tagNames パラメータが設定されていること");
        List<?> tagNames = (List<?>) tagNamesParam;
        assertEquals(2, tagNames.size());
        assertTrue(tagNames.contains("AI"));
        assertTrue(tagNames.contains("設計"));
    }

    /**
     * 【正常系】データ行が存在する場合、正しくDTOへマッピングされること。
     * - id, title, summary, author, createdAt, score が設定されること
     * - body が200文字を超える場合は先頭200文字＋"..."で切り詰めること
     * - タグ一覧がアイテムに紐付くこと
     */
    @Test
    void search_withDataRows_mapsRowsToDto() {
        stubCount(1L);

        String longBody = "A".repeat(250); // 250文字 → 200文字で切り詰め
        Map<String, Object> row = buildRow(1L, "テスト記事", longBody);

        // 1回目: データクエリ、2回目: タグクエリ
        Map<String, Object> tagRow = new HashMap<>();
        tagRow.put("knowledge_id", 1L);
        tagRow.put("name", "Spring");

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(List.of(row))   // データクエリ
                .thenReturn(List.of(tagRow)); // タグクエリ

        KnowledgeSearchRequest req = new KnowledgeSearchRequest();
        KnowledgePageResponse result = service.search(req);

        assertEquals(1, result.getItems().size());
        KnowledgeItemDto item = result.getItems().get(0);

        assertEquals(1L, item.getId());
        assertEquals("テスト記事", item.getTitle());
        // 本文が200文字で切り詰められ "..." が付加されること
        assertEquals(200 + 3, item.getSummary().length()); // 200字 + "..."(3字)
        assertTrue(item.getSummary().endsWith("..."));
        assertEquals("testuser", item.getAuthor());
        assertEquals(5L, item.getScore()); // like_count
        assertEquals(List.of("Spring"), item.getTags());
    }
}
