package com.ginga.naviai.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectRequest {
    @NotBlank
    private String reason;
    public RejectRequest() {}
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
