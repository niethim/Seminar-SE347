package com.demo.voucher.rest;

import com.demo.voucher.exception.DuplicateClaimException;
import com.demo.voucher.exception.FraudDetectedException;
import com.demo.voucher.exception.RateLimitExceededException;
import com.demo.voucher.exception.StockExhaustedException;
import com.demo.voucher.exception.VoucherNotFoundException;
import com.demo.voucher.interceptor.TraceContext;
import com.demo.voucher.rest.dto.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public class AppExceptionMappers {

    @Provider
    public static class StockExhaustedMapper implements ExceptionMapper<StockExhaustedException> {
        @Inject
        private TraceContext traceContext;

        @Override
        public Response toResponse(StockExhaustedException e) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage(), traceContext.getTraceId()))
                .build();
        }
    }

    @Provider
    public static class DuplicateClaimMapper implements ExceptionMapper<DuplicateClaimException> {
        @Inject
        private TraceContext traceContext;

        @Override
        public Response toResponse(DuplicateClaimException e) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage(), traceContext.getTraceId()))
                .build();
        }
    }

    @Provider
    public static class FraudDetectedMapper implements ExceptionMapper<FraudDetectedException> {
        @Inject
        private TraceContext traceContext;

        @Override
        public Response toResponse(FraudDetectedException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage(), traceContext.getTraceId()))
                .build();
        }
    }

    @Provider
    public static class RateLimitExceededMapper implements ExceptionMapper<RateLimitExceededException> {
        @Inject
        private TraceContext traceContext;

        @Override
        public Response toResponse(RateLimitExceededException e) {
            return Response.status(429) // 429 Too Many Requests
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage(), traceContext.getTraceId()))
                .build();
        }
    }

    @Provider
    public static class VoucherNotFoundMapper implements ExceptionMapper<VoucherNotFoundException> {
        @Inject
        private TraceContext traceContext;

        @Override
        public Response toResponse(VoucherNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(e.getMessage(), traceContext.getTraceId()))
                .build();
        }
    }
}
