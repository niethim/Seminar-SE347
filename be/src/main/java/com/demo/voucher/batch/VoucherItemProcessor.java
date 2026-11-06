package com.demo.voucher.batch;

import com.demo.voucher.model.VoucherClaim;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import java.math.BigDecimal;

@Named("voucherItemProcessor")
@Dependent
public class VoucherItemProcessor implements ItemProcessor {

    private static final BigDecimal DEFAULT_VOUCHER_VALUE = new BigDecimal("25.00");

    @Override
    public Object processItem(Object item) throws Exception {
        if (!(item instanceof VoucherClaim)) {
            return null;
        }

        VoucherClaim claim = (VoucherClaim) item;
        return new ProcessedClaim(claim.getClaimCode(), DEFAULT_VOUCHER_VALUE);
    }
}
