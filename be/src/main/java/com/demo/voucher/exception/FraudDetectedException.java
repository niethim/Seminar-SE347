package com.demo.voucher.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class FraudDetectedException extends RuntimeException {
    public FraudDetectedException(String message) {
        super(message);
    }
}
