package com.demo.voucher.service;

import com.demo.voucher.interceptor.Traced;
import com.demo.voucher.model.ClaimStatus;
import com.demo.voucher.model.VoucherClaim;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

@Stateless
public class VoucherFulfillmentService {

    private static final Logger LOGGER = Logger.getLogger(VoucherFulfillmentService.class.getName());

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void fulfillClaim(String claimCode, String traceId) {
        VoucherClaim claim = em.createQuery(
                "SELECT c FROM VoucherClaim c WHERE c.claimCode = :claimCode", VoucherClaim.class)
            .setParameter("claimCode", claimCode)
            .getResultStream().findFirst().orElse(null);

        if (claim == null) {
            LOGGER.warning(String.format("[Trace: %s] Claim %s not found for fulfillment.", traceId, claimCode));
            return;
        }

        // Idempotency check against redelivery
        if (claim.getStatus() == ClaimStatus.ISSUED) {
            LOGGER.info(String.format("[Trace: %s] Claim %s already ISSUED. Redelivery acknowledged safely.", traceId, claimCode));
            return;
        }

        // Simulate asynchronous certificate/voucher generation (e.g. PDF generation)
        LOGGER.info(String.format("[Trace: %s] Generating digital certificate for claim: %s (User: %s)...",
            traceId, claimCode, claim.getUserId()));

        claim.setStatus(ClaimStatus.ISSUED);
        em.merge(claim);

        LOGGER.info(String.format("[Trace: %s] Claim %s successfully updated to ISSUED.", traceId, claimCode));
    }
}
