package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserUpdateRequest {
    private String role;
    @JsonProperty("is_active")
    private Boolean isActive;
    private String reason;
    public UserUpdateRequest() {}
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
