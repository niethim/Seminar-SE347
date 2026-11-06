package com.demo.voucher.batch;

import com.demo.voucher.model.ClaimStatus;
import com.demo.voucher.model.VoucherClaim;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

@Named("voucherItemReader")
@Dependent
public class VoucherItemReader extends AbstractItemReader {

    @PersistenceContext(unitName = "VoucherPU")
    private EntityManager em;

    private int offset = 0;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        if (checkpoint instanceof Integer) {
            this.offset = (Integer) checkpoint;
        } else {
            this.offset = 0;
        }
    }

    @Override
    public Object readItem() throws Exception {
        List<VoucherClaim> results = em.createQuery(
                "SELECT c FROM VoucherClaim c WHERE c.status = :status ORDER BY c.id ASC",
                VoucherClaim.class)
            .setParameter("status", ClaimStatus.ISSUED)
            .setFirstResult(offset)
            .setMaxResults(1)
            .getResultList();

        if (results.isEmpty()) {
            return null; // Signals end of data to chunk processor
        }

        offset++;
        return results.get(0);
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return offset;
    }
}
