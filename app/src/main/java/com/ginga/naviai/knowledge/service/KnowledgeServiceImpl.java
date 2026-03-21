package com.ginga.naviai.knowledge.service;

import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.knowledge.dto.AttachmentDto;
import com.ginga.naviai.knowledge.dto.AuthorDto;
import com.ginga.naviai.knowledge.dto.CommentDto;
import com.ginga.naviai.knowledge.dto.KnowledgeDetailResponse;
import com.ginga.naviai.knowledge.dto.KnowledgeResponse;
import com.ginga.naviai.knowledge.dto.MetaDto;
import com.ginga.naviai.knowledge.dto.RevisionDto;
import com.ginga.naviai.knowledge.dto.TagDto;
import com.ginga.naviai.knowledge.entity.Attachment;
import com.ginga.naviai.knowledge.entity.Comment;
import com.ginga.naviai.knowledge.entity.Knowledge;
import com.ginga.naviai.knowledge.entity.KnowledgeRevision;
import com.ginga.naviai.knowledge.repository.AttachmentRepository;
import com.ginga.naviai.knowledge.repository.CommentRepository;
import com.ginga.naviai.knowledge.repository.KnowledgeRepository;
import com.ginga.naviai.knowledge.repository.KnowledgeRevisionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public KnowledgeServiceImpl(KnowledgeRepository knowledgeRepository,
                                CommentRepository commentRepository,
                                AttachmentRepository attachmentRepository,
                                KnowledgeRevisionRepository revisionRepository,
                                UserRepository userRepository) {
        this.knowledgeRepository = knowledgeRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.revisionRepository = revisionRepository;
        this.userRepository = userRepository;
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
}
