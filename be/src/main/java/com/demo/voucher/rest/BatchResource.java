package com.demo.voucher.rest;

import com.demo.voucher.interceptor.TraceContext;
import com.demo.voucher.interceptor.Traced;
import com.demo.voucher.model.BatchClearingRecord;
import com.demo.voucher.rest.dto.BatchExecutionResponse;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Properties;

@Path("/batch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BatchResource {

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    @Inject
    private TraceContext traceContext;

    @POST
    @Path("/reconciliation")
    @Traced
    public Response startReconciliation() {
        return executeJob();
    }

    @POST
    @Path("/reconcile")
    @Traced
    public Response startReconcileAlias() {
        return executeJob();
    }

    private Response executeJob() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParameters = new Properties();
        jobParameters.setProperty("traceId", traceContext.getTraceId());
        jobParameters.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));

        long executionId = jobOperator.start("reconciliationJob", jobParameters);
        JobExecution execution = jobOperator.getJobExecution(executionId);

        return Response.status(Response.Status.ACCEPTED).entity(new BatchExecutionResponse(
            executionId,
            execution.getJobName(),
            execution.getBatchStatus() != null ? execution.getBatchStatus().toString() : "STARTING",
            execution.getExitStatus(),
            execution.getStartTime(),
            execution.getEndTime()
        )).build();
    }

    @GET
    @Path("/reconciliation/{executionId}")
    @Traced
    public Response getExecutionStatus(@PathParam("executionId") long executionId) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution execution = jobOperator.getJobExecution(executionId);

        if (execution == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(new BatchExecutionResponse(
            executionId,
            execution.getJobName(),
            execution.getBatchStatus() != null ? execution.getBatchStatus().toString() : "UNKNOWN",
            execution.getExitStatus(),
            execution.getStartTime(),
            execution.getEndTime()
        )).build();
    }

    @GET
    @Path("/records")
    @Traced
    public Response getClearingRecords() {
        List<BatchClearingRecord> records = em.createQuery(
                "SELECT b FROM BatchClearingRecord b ORDER BY b.createdAt DESC", BatchClearingRecord.class)
            .setMaxResults(20)
            .getResultList();

        return Response.ok(records).build();
    }
}
