package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.knowledge.dto.KnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 記事検索サービスの実装クラス。
 * NamedParameterJdbcTemplate を用いて動的SQLを構築し、記事の検索・ページング・タグ取得を行う。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public KnowledgePageResponse search(KnowledgeSearchRequest request) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        // ── WHERE 句の構築 ──────────────────────────────────
        StringBuilder where = new StringBuilder(
                "WHERE k.is_deleted = false AND k.status = 'published'"
        );

        // 全文検索：タイトル・本文の部分一致（大文字小文字を無視）
        if (request.getQ() != null && !request.getQ().isBlank()) {
            where.append(" AND (LOWER(k.title) LIKE LOWER(:q) OR LOWER(k.body) LIKE LOWER(:q))");
            params.addValue("q", "%" + request.getQ() + "%");
        }

        // タグフィルタ：指定タグのいずれかを持つ記事のみに絞り込む
        List<String> tagList = parseTags(request.getTags());
        if (!tagList.isEmpty()) {
            where.append(
                    " AND k.id IN (" +
                    "  SELECT kt.knowledge_id FROM knowledge_tag kt" +
                    "  JOIN tag t ON kt.tag_id = t.id" +
                    "  WHERE t.name IN (:tagNames)" +
                    ")"
            );
            params.addValue("tagNames", tagList);
        }

        // ── ORDER BY 句の決定 ────────────────────────────────
        // sort パラメータが明示されている場合は filter より優先する
        String orderBy = resolveOrderBy(request);

        // filter=latest の場合は最大20件に制限
        int effectiveSize = "latest".equals(request.getFilter())
                ? Math.min(request.getSize(), 20)
                : request.getSize();

        // ── 総件数クエリ ──────────────────────────────────────
        String countSql = "SELECT COUNT(DISTINCT k.id) FROM knowledge k " + where;
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long totalElements = (total != null) ? total : 0L;

        // ── データ取得クエリ ──────────────────────────────────
        // like_count はコリレートサブクエリで算出（"like" はSQL予約語のため引用符でエスケープ）
        String dataSql =
                "SELECT k.id, k.title, k.body, u.username AS author, k.created_at," +
                " (SELECT COUNT(*) FROM \"like\" l WHERE l.knowledge_id = k.id) AS like_count" +
                " FROM knowledge k" +
                " LEFT JOIN users u ON k.author_id = u.id " +
                where +
                " ORDER BY " + orderBy +
                " LIMIT :size OFFSET :offset";

        params.addValue("size", effectiveSize);
        params.addValue("offset", (long) request.getPage() * effectiveSize);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, params);

        if (rows.isEmpty()) {
            return KnowledgePageResponse.builder()
                    .page(request.getPage())
                    .size(effectiveSize)
                    .totalElements(totalElements)
                    .items(Collections.emptyList())
                    .build();
        }

        // 対象IDを抽出し、タグを一括取得
        List<Long> ids = rows.stream()
                .map(row -> ((Number) row.get("id")).longValue())
                .collect(Collectors.toList());
        Map<Long, List<String>> tagMap = fetchTagsForIds(ids);

        // DTO へのマッピング
        List<KnowledgeItemDto> items = rows.stream()
                .map(row -> {
                    Long id = ((Number) row.get("id")).longValue();
                    return KnowledgeItemDto.builder()
                            .id(id)
                            .title((String) row.get("title"))
                            .summary(buildSummary((String) row.get("body")))
                            .author((String) row.get("author"))
                    .createdAt(toInstant(row.get("created_at")))
                            .score(((Number) row.get("like_count")).longValue())
                            .tags(tagMap.getOrDefault(id, Collections.emptyList()))
                            .build();
                })
                .collect(Collectors.toList());

        return KnowledgePageResponse.builder()
                .page(request.getPage())
                .size(effectiveSize)
                .totalElements(totalElements)
                .items(items)
                .build();
    }

    /**
     * filter / sort パラメータから ORDER BY 句の文字列を決定する。
     * sort が明示されている場合は filter より優先される。
     *
     * @param request 検索リクエスト
     * @return ORDER BY に続く列指定文字列（例: "k.created_at DESC"）
     */
    private String resolveOrderBy(KnowledgeSearchRequest request) {
        if (request.getSort() != null) {
            return switch (request.getSort()) {
                case "created_at", "-created_at" -> "k.created_at DESC";
                case "score" -> "like_count DESC";
                default -> "k.created_at DESC";
            };
        }
        // filter=recommended → いいね数の多い順
        if ("recommended".equals(request.getFilter())) {
            return "like_count DESC";
        }
        // filter=latest またはデフォルト → 作成日時降順
        return "k.created_at DESC";
    }

    /**
     * カンマ区切りのタグ文字列をリストに変換する。
     * 空文字・空白のみのエントリは除外する。
     */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 記事本文の先頭200文字を要約テキストとして返す。
     * 200文字を超える場合は "..." を付加する。
     */
    private String buildSummary(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    /**
     * 指定されたknowledge_idリストに対応するタグを一括取得し、
     * knowledge_id → タグ名リスト のマップを返す。
     */
    private Map<Long, List<String>> fetchTagsForIds(List<Long> ids) {
        String tagSql =
                "SELECT kt.knowledge_id, t.name" +
                " FROM knowledge_tag kt" +
                " JOIN tag t ON kt.tag_id = t.id" +
                " WHERE kt.knowledge_id IN (:ids)";
        MapSqlParameterSource tagParams = new MapSqlParameterSource("ids", ids);
        List<Map<String, Object>> tagRows = jdbcTemplate.queryForList(tagSql, tagParams);

        Map<Long, List<String>> tagMap = new HashMap<>();
        for (Map<String, Object> row : tagRows) {
            Long kid = ((Number) row.get("knowledge_id")).longValue();
            String name = (String) row.get("name");
            tagMap.computeIfAbsent(kid, k -> new ArrayList<>()).add(name);
        }
        return tagMap;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        throw new IllegalArgumentException("Unsupported datetime value type: " + value.getClass().getName());
    }
}
