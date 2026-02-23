package com.ginga.naviai.knowledge.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "knowledge")
public class Knowledge {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeStatus status = KnowledgeStatus.DRAFT;

    @ElementCollection
    @CollectionTable(name = "knowledge_tags", joinColumns = @JoinColumn(name = "knowledge_id"))
    @Column(name = "tag", length = 50)
    private List<String> tags = new ArrayList<>();

    @Column(length = 100)
    private String category;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "declined_reason", length = 2000)
    private String declinedReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Knowledge() {}

    @PrePersist
    public void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public KnowledgeStatus getStatus() { return status; }
    public void setStatus(KnowledgeStatus status) { this.status = status; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getDeclinedReason() { return declinedReason; }
    public void setDeclinedReason(String declinedReason) { this.declinedReason = declinedReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
