package com.demo.voucher.service;

import com.demo.voucher.exception.DuplicateClaimException;
import com.demo.voucher.exception.FraudDetectedException;
import com.demo.voucher.exception.StockExhaustedException;
import com.demo.voucher.exception.VoucherNotFoundException;
import com.demo.voucher.interceptor.Traced;
import com.demo.voucher.model.ClaimStatus;
import com.demo.voucher.model.Voucher;
import com.demo.voucher.model.VoucherClaim;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
public class VoucherClaimService {

    private static final Logger LOGGER = Logger.getLogger(VoucherClaimService.class.getName());

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    @Resource(lookup = "java:/jms/queue/VoucherIssuanceQueue")
    private Queue issuanceQueue;

    @Inject
    private JMSContext jmsContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public VoucherClaim claimVoucher(String voucherCode, String userId, boolean simulateFraud, String traceId) {
        LOGGER.info(String.format("[Trace: %s] Attempting claim for voucher: %s, user: %s, simulateFraud: %b",
            traceId, voucherCode, userId, simulateFraud));

        // 1. Pessimistic lock on Voucher to eliminate race conditions under concurrent claims
        Voucher voucher;
        try {
            voucher = em.createQuery("SELECT v FROM Voucher v WHERE v.code = :code", Voucher.class)
                .setParameter("code", voucherCode)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new VoucherNotFoundException("Voucher not found: " + voucherCode);
        }

        // 2. Validate stock availability
        if (voucher.getAvailableStock() <= 0) {
            throw new StockExhaustedException("Stock exhausted for voucher: " + voucherCode);
        }

        // 3. Application check for idempotency
        Long existingClaims = em.createQuery(
                "SELECT COUNT(c) FROM VoucherClaim c WHERE c.voucher.id = :voucherId AND c.userId = :userId", Long.class)
            .setParameter("voucherId", voucher.getId())
            .setParameter("userId", userId)
            .getSingleResult();

        if (existingClaims > 0) {
            throw new DuplicateClaimException("User " + userId + " has already claimed voucher " + voucherCode);
        }

        // 4. Decrement available stock
        voucher.setAvailableStock(voucher.getAvailableStock() - 1);

        // 5. Create claim record with unique claim code
        String claimCode = "CLM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        VoucherClaim claim = new VoucherClaim(
            voucher,
            userId,
            claimCode,
            ClaimStatus.CLAIMED,
            traceId,
            LocalDateTime.now()
        );
        em.persist(claim);

        // Flush immediately to trigger DB unique constraints inside this transaction
        em.flush();

        // 6. Simulate fraud if requested (triggers container-managed rollback)
        if (simulateFraud) {
            LOGGER.warning(String.format("[Trace: %s] Fraud simulation triggered for user: %s. Rolling back.", traceId, userId));
            throw new FraudDetectedException("Fraud detected during claim validation for user: " + userId);
        }

        // 7. Transactionally publish issuance message to JMS queue
        JsonObject payload = Json.createObjectBuilder()
            .add("claimCode", claimCode)
            .add("voucherCode", voucherCode)
            .add("userId", userId)
            .add("traceId", traceId)
            .build();

        jmsContext.createProducer()
            .setProperty("traceId", traceId)
            .send(issuanceQueue, payload.toString());

        LOGGER.info(String.format("[Trace: %s] Voucher claim created and JMS message dispatched: %s", traceId, claimCode));
        return claim;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Voucher getVoucher(String code) {
        try {
            return em.createQuery("SELECT v FROM Voucher v WHERE v.code = :code", Voucher.class)
                .setParameter("code", code)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new VoucherNotFoundException("Voucher not found: " + code);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public VoucherClaim getClaim(String voucherCode, String claimCode) {
        try {
            return em.createQuery(
                    "SELECT c FROM VoucherClaim c JOIN FETCH c.voucher WHERE c.claimCode = :claimCode AND c.voucher.code = :voucherCode",
                    VoucherClaim.class)
                .setParameter("claimCode", claimCode)
                .setParameter("voucherCode", voucherCode)
                .getSingleResult();
        } catch (NoResultException e) {
            throw new VoucherNotFoundException("Claim record not found: " + claimCode);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Voucher initOrResetVoucher(String code, int stock) {
        Voucher voucher = em.createQuery("SELECT v FROM Voucher v WHERE v.code = :code", Voucher.class)
            .setParameter("code", code)
            .getResultStream().findFirst().orElse(null);

        if (voucher == null) {
            voucher = new Voucher(code, stock, stock);
            em.persist(voucher);
        } else {
            voucher.setTotalStock(stock);
            voucher.setAvailableStock(stock);
        }
        em.createQuery("DELETE FROM VoucherClaim c WHERE c.voucher.code = :code")
            .setParameter("code", code)
            .executeUpdate();
        em.flush();
        return voucher;
    }
}
