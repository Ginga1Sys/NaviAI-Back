package com.ginga.naviai.admin.dto;

public class StatsResponse {
    private int pending;
    private int published;
    private int declined;
    public StatsResponse() {}
    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }
    public int getPublished() { return published; }
    public void setPublished(int published) { this.published = published; }
    public int getDeclined() { return declined; }
    public void setDeclined(int declined) { this.declined = declined; }
}
