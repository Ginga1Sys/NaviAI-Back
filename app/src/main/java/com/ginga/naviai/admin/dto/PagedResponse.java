package com.ginga.naviai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PagedResponse<T> {
    private List<T> data;
    private Meta meta;
    public static class Meta {
        private int page;
        @JsonProperty("per_page")
        private int perPage;
        private long total;
        public Meta() {}
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getPerPage() { return perPage; }
        public void setPerPage(int perPage) { this.perPage = perPage; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
    }

    public PagedResponse() {}
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }
}
