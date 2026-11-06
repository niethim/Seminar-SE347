package com.demo.voucher.rest.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ClaimResponse implements Serializable {
    private String claimCode;
    private String voucherCode;
    private String userId;
    private String status;
    private String traceId;
    private LocalDateTime createdAt;

    public ClaimResponse() {
    }

    public ClaimResponse(String claimCode, String voucherCode, String userId, String status, String traceId, LocalDateTime createdAt) {
        this.claimCode = claimCode;
        this.voucherCode = voucherCode;
        this.userId = userId;
        this.status = status;
        this.traceId = traceId;
        this.createdAt = createdAt;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public void setClaimCode(String claimCode) {
        this.claimCode = claimCode;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
