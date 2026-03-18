package com.ginga.naviai.admin.dto;

import java.util.List;

public class BulkActionResultResponse {
    private String action;
    private List<BulkItemResult> results;
    private Summary summary;

    public static class BulkItemResult {
        private String id;
        private boolean ok;
        private ErrorDetail error;
        public BulkItemResult() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
        public ErrorDetail getError() { return error; }
        public void setError(ErrorDetail error) { this.error = error; }
    }

    public static class Summary {
        private int ok;
        private int failed;
        public Summary() {}
        public int getOk() { return ok; }
        public void setOk(int ok) { this.ok = ok; }
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
    }

    public static class ErrorDetail {
        private String code;
        private String message;
        public ErrorDetail() {}
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public BulkActionResultResponse() {}
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public List<BulkItemResult> getResults() { return results; }
    public void setResults(List<BulkItemResult> results) { this.results = results; }
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
}
