package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleStatusResponse {
    private String id;
    private String status;
    @JsonProperty("published_at")
    private String publishedAt;
    public SimpleStatusResponse() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
}
