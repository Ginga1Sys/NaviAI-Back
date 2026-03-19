package com.ginga.naviai.knowledge.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommentDto {
    private String id;
    private String knowledgeId;
    private AuthorDto author;
    private String body;
    /** null の場合はルートコメント */
    private String parentCommentId;
    private Boolean isDeleted;
    private String createdAt;
}
