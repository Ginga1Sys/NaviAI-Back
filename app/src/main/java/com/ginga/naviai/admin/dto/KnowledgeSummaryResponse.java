package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class KnowledgeSummaryResponse {
    private String id;
    private String title;
    private String summary;
    private UserBrief author;
    private String status;
    private String category;
    @JsonProperty("submitted_at")
    private Instant submittedAt;

    public KnowledgeSummaryResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public UserBrief getAuthor() { return author; }
    public void setAuthor(UserBrief author) { this.author = author; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
