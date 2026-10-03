package uz.script.wincrm.suppliers.response;

import lombok.Builder;
import lombok.Data;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SupplierBalanceResponse {

    private Long id;

    private Long supplierId;

    private String supplierName;

    private Currency currency;

    private BigDecimal totalPurchase;

    private BigDecimal totalPaid;

    private BigDecimal totalDebt;

    private LocalDateTime lastUpdated;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}