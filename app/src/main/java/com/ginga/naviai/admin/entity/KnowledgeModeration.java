package com.ginga.naviai.admin.entity;

import jakarta.persistence.*;

import java.time.Instant;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "knowledge_moderation")
public class KnowledgeModeration implements Persistable<String> {

    @Id
    @Column(name = "knowledge_id", length = 36)
    private String knowledgeId;

    @Transient
    private boolean isNew = true;

    @Lob
    @Column(name = "internal_note")
    private String internalNote;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public KnowledgeModeration() {}

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() { return knowledgeId; }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getKnowledgeId() { return knowledgeId; }
    public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
