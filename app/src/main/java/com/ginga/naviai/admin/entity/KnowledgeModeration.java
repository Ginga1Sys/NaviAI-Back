package com.ginga.naviai.admin.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "knowledge_moderation")
public class KnowledgeModeration {

    @Id
    @Column(name = "knowledge_id", length = 36)
    private String knowledgeId;

    @Lob
    @Column(name = "internal_note")
    private String internalNote;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public KnowledgeModeration() {}

    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
