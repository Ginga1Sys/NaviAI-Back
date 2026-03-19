package com.ginga.naviai.knowledge.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Builder;
import lombok.Data;

/** 添付ファイル情報DTO（attachment テーブル実装後に拡張予定） */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AttachmentDto {
    private String id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String storagePath;
    private String uploadedAt;
}
