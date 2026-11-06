package com.demo.voucher.rest;

import com.demo.voucher.model.AuditLog;
import com.demo.voucher.service.AuditService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
public class PublicAuditResource {

    @Inject
    private AuditService auditService;

    @GET
    public Response getRecentLogs(@QueryParam("limit") @DefaultValue("50") int limit) {
        List<AuditLog> logs = auditService.getRecentLogs(limit);
        return Response.ok(logs).build();
    }
}
