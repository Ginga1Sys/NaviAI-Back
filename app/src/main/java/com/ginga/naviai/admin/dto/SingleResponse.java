package com.ginga.naviai.admin.dto;

public class SingleResponse<T> {
    private T data;
    public SingleResponse() {}
    public SingleResponse(T data) { this.data = data; }
    public static <T> SingleResponse<T> of(T data) { return new SingleResponse<>(data); }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
