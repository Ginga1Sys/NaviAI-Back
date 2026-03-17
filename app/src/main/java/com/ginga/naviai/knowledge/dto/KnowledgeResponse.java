package com.ginga.naviai.knowledge.dto;

public class KnowledgeResponse {
    private String id;
    private String title;
    private String excerpt;
    private String date;
    private String status;
    private String thumbnail;

    public KnowledgeResponse(String id, String title, String excerpt, String date, String status, String thumbnail) {
        this.id = id;
        this.title = title;
        this.excerpt = excerpt;
        this.date = date;
        this.status = status;
        this.thumbnail = thumbnail;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
