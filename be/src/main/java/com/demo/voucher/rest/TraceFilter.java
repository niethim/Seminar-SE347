package com.demo.voucher.rest;

import com.demo.voucher.interceptor.TraceContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class TraceFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";

    @Inject
    private TraceContext traceContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String headerTraceId = requestContext.getHeaderString(TRACE_HEADER);
        if (headerTraceId != null && !headerTraceId.isBlank()) {
            traceContext.setTraceId(headerTraceId.trim());
        }
        // Ensure initialized
        String traceId = traceContext.getTraceId();
        requestContext.setProperty(TRACE_HEADER, traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String traceId = traceContext.getTraceId();
        if (traceId != null) {
            responseContext.getHeaders().putSingle(TRACE_HEADER, traceId);
        }
    }
}
