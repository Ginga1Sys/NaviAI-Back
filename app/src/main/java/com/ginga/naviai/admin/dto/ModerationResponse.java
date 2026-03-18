package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ModerationResponse {
    @JsonProperty("knowledge_id")
    private String knowledgeId;
    @JsonProperty("internal_note")
    private String internalNote;
    @JsonProperty("updated_by")
    private UserBrief updatedBy;
    @JsonProperty("updated_at")
    private Instant updatedAt;
    public ModerationResponse() {}
    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public UserBrief getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserBrief updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
