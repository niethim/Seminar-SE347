package com.demo.voucher.rest;

import com.demo.voucher.interceptor.TraceContext;
import com.demo.voucher.interceptor.Traced;
import com.demo.voucher.model.Voucher;
import com.demo.voucher.model.VoucherClaim;
import com.demo.voucher.rest.dto.ClaimRequest;
import com.demo.voucher.rest.dto.ClaimResponse;
import com.demo.voucher.rest.dto.VoucherResponse;
import com.demo.voucher.service.RateLimiterService;
import com.demo.voucher.service.VoucherClaimService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/vouchers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VoucherResource {

    @Inject
    private VoucherClaimService claimService;

    @Inject
    private RateLimiterService rateLimiterService;

    @Inject
    private TraceContext traceContext;

    @GET
    @Path("/{code}")
    @Traced
    public Response getVoucher(@PathParam("code") String code) {
        Voucher voucher = claimService.getVoucher(code);
        return Response.ok(new VoucherResponse(
            voucher.getCode(),
            voucher.getTotalStock(),
            voucher.getAvailableStock()
        )).build();
    }

    @POST
    @Path("/{code}/claim")
    @Traced
    public Response claimVoucher(@PathParam("code") String code, ClaimRequest request) {
        String userId = (request != null && request.getUserId() != null && !request.getUserId().isBlank())
            ? request.getUserId().trim()
            : "user-" + System.currentTimeMillis();

        boolean simulateFraud = request != null && request.isSimulateFraud();

        // 1. Rate limiter check before touching transactional resources
        rateLimiterService.checkRateLimit(userId);

        // 2. Perform transactional claim under pessimistic lock
        VoucherClaim claim = claimService.claimVoucher(code, userId, simulateFraud, traceContext.getTraceId());

        // 3. Return 202 Accepted with claim details
        ClaimResponse response = new ClaimResponse(
            claim.getClaimCode(),
            code,
            claim.getUserId(),
            claim.getStatus().name(),
            claim.getTraceId(),
            claim.getCreatedAt()
        );
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @GET
    @Path("/{code}/claims/{claimCode}")
    @Traced
    public Response getClaim(@PathParam("code") String code, @PathParam("claimCode") String claimCode) {
        VoucherClaim claim = claimService.getClaim(code, claimCode);
        return Response.ok(new ClaimResponse(
            claim.getClaimCode(),
            code,
            claim.getUserId(),
            claim.getStatus().name(),
            claim.getTraceId(),
            claim.getCreatedAt()
        )).build();
    }

    @POST
    @Path("/{code}/init")
    @Traced
    public Response initVoucher(
            @PathParam("code") String code,
            @QueryParam("stock") @DefaultValue("50") int stock) {
        Voucher voucher = claimService.initOrResetVoucher(code, stock);
        return Response.ok(new VoucherResponse(
            voucher.getCode(),
            voucher.getTotalStock(),
            voucher.getAvailableStock()
        )).build();
    }
}
