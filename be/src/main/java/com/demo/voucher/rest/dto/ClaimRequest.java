package com.demo.voucher.rest.dto;

import java.io.Serializable;

public class ClaimRequest implements Serializable {
    private String userId;
    private boolean simulateFraud;

    public ClaimRequest() {
    }

    public ClaimRequest(String userId, boolean simulateFraud) {
        this.userId = userId;
        this.simulateFraud = simulateFraud;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSimulateFraud() {
        return simulateFraud;
    }

    public void setSimulateFraud(boolean simulateFraud) {
        this.simulateFraud = simulateFraud;
    }
}
