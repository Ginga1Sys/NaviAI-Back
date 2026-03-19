package com.ginga.naviai.knowledge.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Builder;
import lombok.Data;

/** 編集履歴DTO */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RevisionDto {
    private String id;
    private String knowledgeId;
    private AuthorDto editor;
    private String title;
    private String body;
    private String diffSummary;
    private String createdAt;
}
