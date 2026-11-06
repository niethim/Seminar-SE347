package com.demo.voucher.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_clearing_records")
public class BatchClearingRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_job_id", nullable = false, length = 64)
    private String batchJobId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_claims", nullable = false)
    private int totalClaims;

    @Column(name = "total_cleared_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalClearedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BatchClearingRecord() {
    }

    public BatchClearingRecord(String batchJobId, LocalDate settlementDate, int totalClaims, BigDecimal totalClearedAmount, LocalDateTime createdAt) {
        this.batchJobId = batchJobId;
        this.settlementDate = settlementDate;
        this.totalClaims = totalClaims;
        this.totalClearedAmount = totalClearedAmount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(String batchJobId) {
        this.batchJobId = batchJobId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(int totalClaims) {
        this.totalClaims = totalClaims;
    }

    public BigDecimal getTotalClearedAmount() {
        return totalClearedAmount;
    }

    public void setTotalClearedAmount(BigDecimal totalClearedAmount) {
        this.totalClearedAmount = totalClearedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
