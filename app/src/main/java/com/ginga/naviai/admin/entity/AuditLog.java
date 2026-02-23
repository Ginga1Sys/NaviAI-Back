package com.ginga.naviai.admin.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id", length = 36)
    private String targetId;

    @Lob
    @Column(name = "detail_json")
    private String detailJson;

    @Column(name = "created_at")
    private Instant createdAt;

    public AuditLog() {}

    @PrePersist
    public void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
