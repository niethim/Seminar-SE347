package com.demo.voucher.batch;

import com.demo.voucher.model.BatchClearingRecord;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Named("voucherItemWriter")
@Dependent
public class VoucherItemWriter extends AbstractItemWriter {

    private static final Logger LOGGER = Logger.getLogger(VoucherItemWriter.class.getName());

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    @Inject
    private JobContext jobContext;

    @Override
    public void writeItems(List<Object> items) throws Exception {
        if (items == null || items.isEmpty()) {
            return;
        }

        String jobId = "job-" + jobContext.getExecutionId();
        int chunkCount = items.size();
        BigDecimal chunkAmount = BigDecimal.ZERO;

        for (Object item : items) {
            if (item instanceof ProcessedClaim) {
                ProcessedClaim claim = (ProcessedClaim) item;
                chunkAmount = chunkAmount.add(claim.getAmount());
            }
        }

        LOGGER.info(String.format("Batch Writer [%s]: writing chunk of %d claims, amount: %s",
            jobId, chunkCount, chunkAmount));

        BatchClearingRecord record = em.createQuery(
                "SELECT b FROM BatchClearingRecord b WHERE b.batchJobId = :jobId", BatchClearingRecord.class)
            .setParameter("jobId", jobId)
            .getResultStream().findFirst().orElse(null);

        if (record == null) {
            record = new BatchClearingRecord(
                jobId,
                LocalDate.now(),
                chunkCount,
                chunkAmount,
                LocalDateTime.now()
            );
            em.persist(record);
        } else {
            record.setTotalClaims(record.getTotalClaims() + chunkCount);
            record.setTotalClearedAmount(record.getTotalClearedAmount().add(chunkAmount));
            em.merge(record);
        }
        em.flush();
    }
}
