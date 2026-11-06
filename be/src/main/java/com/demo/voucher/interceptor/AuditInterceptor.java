package com.demo.voucher.interceptor;

import com.demo.voucher.service.AuditService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.security.enterprise.SecurityContext;
import java.security.Principal;

@Interceptor
@Traced
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    @Inject
    private TraceContext traceContext;

    @Inject
    private AuditService auditService;

    @Inject
    private SecurityContext securityContext;

    @AroundInvoke
    public Object audit(InvocationContext ctx) throws Exception {
        long start = System.currentTimeMillis();
        String action = ctx.getTarget().getClass().getSimpleName() + "." + ctx.getMethod().getName();
        String traceId = traceContext.getTraceId();
        String principal = "anonymous";

        try {
            if (securityContext != null && securityContext.getCallerPrincipal() != null) {
                principal = securityContext.getCallerPrincipal().getName();
            }
        } catch (Exception ignored) {
            // Not in a security context or anonymous
        }

        try {
            return ctx.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            auditService.record(traceId, principal, action, duration);
        }
    }
}
