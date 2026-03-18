package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModerationRequest {
    @JsonProperty("internal_note")
    private String internalNote;
    public ModerationRequest() {}
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
}
