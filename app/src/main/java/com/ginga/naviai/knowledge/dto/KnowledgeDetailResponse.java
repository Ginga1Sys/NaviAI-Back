package com.ginga.naviai.knowledge.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * GET /api/v1/knowledge/{id} レスポンスDTO。
 * フロントモックの ArticleDetail 型に対応。
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KnowledgeDetailResponse {
    private String id;
    private String title;
    private String body;
    /** draft / pending / published / declined / archived */
    private String status;
    private Boolean isDeleted;
    private String publishedAt;
    private AuthorDto author;
    private List<AttachmentDto> attachments;
    private List<TagDto> tags;
    private long likesCount;
    private Boolean likedByCurrentUser;
    private List<CommentDto> comments;
    private List<RevisionDto> revisions;
    private MetaDto meta;
}
