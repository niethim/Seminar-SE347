package com.demo.voucher.rest.dto;

import java.io.Serializable;

public class VoucherResponse implements Serializable {
    private String code;
    private int totalStock;
    private int availableStock;

    public VoucherResponse() {
    }

    public VoucherResponse(String code, int totalStock, int availableStock) {
        this.code = code;
        this.totalStock = totalStock;
        this.availableStock = availableStock;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }
}
