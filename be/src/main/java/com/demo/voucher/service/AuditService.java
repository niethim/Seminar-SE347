package com.demo.voucher.service;

import com.demo.voucher.model.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class AuditService {

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void record(String traceId, String principalName, String action, long executionTimeMs) {
        try {
            AuditLog log = new AuditLog(
                traceId,
                principalName != null ? principalName : "anonymous",
                action,
                executionTimeMs,
                LocalDateTime.now()
            );
            em.persist(log);
        } catch (Exception e) {
            // Keep business transaction unaffected if audit logging fails
            // In a production system, fallback to logger
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<AuditLog> getRecentLogs(int limit) {
        return em.createQuery("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC", AuditLog.class)
            .setMaxResults(limit > 0 ? limit : 50)
            .getResultList();
    }
}
