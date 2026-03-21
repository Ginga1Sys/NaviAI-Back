package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.knowledge.dto.AttachmentDto;
import com.ginga.naviai.knowledge.dto.AuthorDto;
import com.ginga.naviai.knowledge.dto.CommentDto;
import com.ginga.naviai.knowledge.dto.KnowledgeDetailResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeSearchRequest;
import com.ginga.naviai.knowledge.dto.MetaDto;
import com.ginga.naviai.knowledge.dto.RevisionDto;
import com.ginga.naviai.knowledge.dto.TagDto;
import com.ginga.naviai.knowledge.dto.KnowledgeItemDto;
import com.ginga.naviai.knowledge.dto.KnowledgePageResponse;
import com.ginga.naviai.knowledge.entity.Attachment;
import com.ginga.naviai.knowledge.entity.Comment;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.KnowledgeRevision;
import com.ginga.naviai.knowledge.repository.AttachmentRepository;
import com.ginga.naviai.knowledge.repository.CommentRepository;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import com.ginga.naviai.knowledge.repository.KnowledgeRevisionRepository;
import com.ginga.naviai.util.DateTimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Tokyo"));
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final KnowledgeRepository knowledgeRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final KnowledgeRevisionRepository revisionRepository;
    private final UserRepository userRepository;
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
                            .createdAt(DateTimeUtils.toInstant(row.get("created_at")))
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

    public KnowledgeServiceImpl(KnowledgeRepository knowledgeRepository,
                                CommentRepository commentRepository,
                                AttachmentRepository attachmentRepository,
                                KnowledgeRevisionRepository revisionRepository,
                                UserRepository userRepository,
                                NamedParameterJdbcTemplate jdbcTemplate) {
        this.knowledgeRepository = knowledgeRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.revisionRepository = revisionRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<KnowledgeResponse> getMyKnowledgeByUsername(String username, Pageable pageable) {
        Page<Knowledge> knowledgePage = knowledgeRepository.findByAuthorUsername(username, pageable);
        return knowledgePage.map(this::convertToKnowledgeResponse);
    }

    @Override
    public Page<KnowledgeResponse> getKnowledgeByAuthorId(Long authorId, Pageable pageable) {
        Page<Knowledge> knowledgePage = knowledgeRepository.findByAuthorId(authorId, pageable);
        return knowledgePage.map(this::convertToKnowledgeResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KnowledgeDetailResponse> getKnowledgeDetail(Long id, String currentUsername) {
        return knowledgeRepository.findByIdAndDeletedFalse(id)
                .map(knowledge -> buildDetailResponse(knowledge, currentUsername));
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private KnowledgeDetailResponse buildDetailResponse(Knowledge knowledge, String currentUsername) {
        // 著者DTO
        AuthorDto authorDto = toAuthorDto(knowledge.getAuthor());

        // タグ一覧
        List<TagDto> tags = knowledge.getTags().stream()
                .map(t -> TagDto.builder()
                        .id(t.getId().toString())
                        .name(t.getName())
                        .build())
                .toList();

        // いいね数
        long likesCount = knowledgeRepository.countLikesByKnowledgeId(knowledge.getId());

        // いいね済み判定（認証ユーザーが存在する場合のみ）
        boolean likedByCurrentUser = false;
        if (currentUsername != null) {
            Optional<User> currentUser = userRepository.findByUsername(currentUsername);
            if (currentUser.isPresent()) {
                likedByCurrentUser = knowledgeRepository.countLikeByUserAndKnowledge(
                        knowledge.getId(), currentUser.get().getId()) > 0;
            }
        }

        // コメント一覧（フラットリスト）
        List<CommentDto> comments = commentRepository
                .findByKnowledgeIdWithAuthor(knowledge.getId())
                .stream()
                .map(c -> toCommentDto(c, knowledge.getId().toString()))
                .toList();

        // 添付ファイル一覧
        List<AttachmentDto> attachments = attachmentRepository
                .findByKnowledgeId(knowledge.getId())
                .stream()
                .map(this::toAttachmentDto)
                .toList();

        // 編集履歴一覧
        List<RevisionDto> revisions = revisionRepository
                .findByKnowledgeIdWithEditor(knowledge.getId())
                .stream()
                .map(r -> toRevisionDto(r, knowledge.getId().toString()))
                .toList();

        MetaDto meta = MetaDto.builder()
                .createdAt(ISO_FORMATTER.format(knowledge.getCreatedAt()))
                .updatedAt(ISO_FORMATTER.format(knowledge.getUpdatedAt()))
                .build();

        String publishedAt = knowledge.getPublishedAt() != null
                ? DATE_FORMATTER.format(knowledge.getPublishedAt())
                : null;

        return KnowledgeDetailResponse.builder()
                .id(knowledge.getId().toString())
                .title(knowledge.getTitle())
                .body(knowledge.getBody())
                .status(knowledge.getStatus())
                .isDeleted(knowledge.isDeleted())
                .publishedAt(publishedAt)
                .author(authorDto)
                .attachments(attachments)
                .tags(tags)
                .likesCount(likesCount)
                .likedByCurrentUser(likedByCurrentUser)
                .comments(comments)
                .revisions(revisions)
                .meta(meta)
                .build();
    }

    private CommentDto toCommentDto(Comment comment, String knowledgeId) {
        return CommentDto.builder()
                .id(comment.getId().toString())
                .knowledgeId(knowledgeId)
                .author(toAuthorDto(comment.getAuthor()))
                .body(comment.getBody())
                .parentCommentId(comment.getParentCommentId() != null
                        ? comment.getParentCommentId().toString() : null)
                .isDeleted(comment.isDeleted())
                .createdAt(DATE_FORMATTER.format(comment.getCreatedAt()))
                .build();
    }

    private AttachmentDto toAttachmentDto(Attachment attachment) {
        return AttachmentDto.builder()
                .id(attachment.getId().toString())
                .filename(attachment.getFilename())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes() != null ? attachment.getSizeBytes() : 0L)
                .storagePath(attachment.getStoragePath())
                .uploadedAt(ISO_FORMATTER.format(attachment.getUploadedAt()))
                .build();
    }

    private RevisionDto toRevisionDto(KnowledgeRevision revision, String knowledgeId) {
        return RevisionDto.builder()
                .id(revision.getId().toString())
                .knowledgeId(knowledgeId)
                .editor(toAuthorDto(revision.getEditor()))
                .title(revision.getTitle())
                .body(revision.getBody())
                .diffSummary(revision.getDiffSummary())
                .createdAt(ISO_FORMATTER.format(revision.getCreatedAt()))
                .build();
    }

    private AuthorDto toAuthorDto(User user) {
        if (user == null) {
            return null;
        }
        return AuthorDto.builder()
                .id(user.getId().toString())
                .name(user.getDisplayName())
                .role(user.getRole())
                .isActive(user.isEnabled())
                .build();
    }

    private KnowledgeResponse convertToKnowledgeResponse(Knowledge knowledge) {
        String statusLabel = switch (knowledge.getStatus()) {
            case "draft" -> "下書き";
            case "pending" -> "レビュー中";
            case "published" -> "公開";
            case "declined" -> "差し戻し";
            default -> knowledge.getStatus();
        };

        String formattedDate = DATE_FORMATTER.format(knowledge.getCreatedAt());

        return new KnowledgeResponse(
                knowledge.getId().toString(),
                knowledge.getTitle(),
                knowledge.getExcerpt(),
                formattedDate,
                statusLabel,
                knowledge.getThumbnail()
        );
    }

    private String resolveOrderBy(KnowledgeSearchRequest request) {
        if (request.getSort() != null) {
            return switch (request.getSort()) {
                case "created_at" -> "k.created_at ASC";
                case "-created_at" -> "k.created_at DESC";
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
}
