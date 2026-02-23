package com.ginga.naviai.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BulkActionRequest {
    @NotNull
    private String action; // approve | reject
    @NotEmpty
    private List<String> ids;
    private String reason;

    public BulkActionRequest() {}
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public List<String> getIds() { return ids; }
    public void setIds(List<String> ids) { this.ids = ids; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
