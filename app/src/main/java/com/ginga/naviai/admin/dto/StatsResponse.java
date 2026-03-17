package com.ginga.naviai.admin.dto;

public class StatsResponse {
    private long pending;
    private long published;
    private long declined;
    public StatsResponse() {}
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public long getPublished() { return published; }
    public void setPublished(long published) { this.published = published; }
    public long getDeclined() { return declined; }
    public void setDeclined(long declined) { this.declined = declined; }
}
