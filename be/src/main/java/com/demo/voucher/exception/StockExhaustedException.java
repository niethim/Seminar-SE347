package com.demo.voucher.exception;

public class StockExhaustedException extends RuntimeException {
    public StockExhaustedException(String message) {
        super(message);
    }
}
