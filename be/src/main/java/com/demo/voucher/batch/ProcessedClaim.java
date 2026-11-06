package com.demo.voucher.batch;

import java.io.Serializable;
import java.math.BigDecimal;

public class ProcessedClaim implements Serializable {
    private final String claimCode;
    private final BigDecimal amount;

    public ProcessedClaim(String claimCode, BigDecimal amount) {
        this.claimCode = claimCode;
        this.amount = amount;
    }

    public String getClaimCode() {
        return claimCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
