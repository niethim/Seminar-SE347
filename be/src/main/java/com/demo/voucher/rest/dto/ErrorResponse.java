package com.demo.voucher.rest.dto;

import java.io.Serializable;

public class ErrorResponse implements Serializable {
    private String error;
    private String traceId;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String traceId) {
        this.error = error;
        this.traceId = traceId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
