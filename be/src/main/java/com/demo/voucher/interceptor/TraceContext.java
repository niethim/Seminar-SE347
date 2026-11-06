package com.demo.voucher.interceptor;

import jakarta.enterprise.context.RequestScoped;
import java.io.Serializable;
import java.util.UUID;

@RequestScoped
public class TraceContext implements Serializable {

    private String traceId;

    public String getTraceId() {
        if (traceId == null || traceId.isBlank()) {
            traceId = "trc-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return traceId;
    }

    public void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            this.traceId = traceId;
        }
    }
}
