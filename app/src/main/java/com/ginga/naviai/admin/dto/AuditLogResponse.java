package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AuditLogResponse {
    private String id;
    private String action;
    private UserBrief actor;
    private Target target;
    private Object detail;
    @JsonProperty("created_at")
    private Instant createdAt;

    public static class Target {
        private String type;
        private String id;
        public Target() {}
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public AuditLogResponse() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public UserBrief getActor() { return actor; }
    public void setActor(UserBrief actor) { this.actor = actor; }
    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }
    public Object getDetail() { return detail; }
    public void setDetail(Object detail) { this.detail = detail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
