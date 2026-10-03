package uz.script.wincrm.warehouse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(name = "Warehouse Order DTO", description = "Warehouse order creation and update request")
public class WarehouseOrderDTO {

    @NotNull(message = "Supplier id is required")
    @Schema(
            description = "Supplier identifier",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long supplierId;

    @NotNull(message = "Warehouse id is required")
    @Schema(
            description = "Warehouse identifier",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long warehouseId;

    @Schema(
            description = "Optional comment about the order",
            example = "Urgent delivery, handle with care"
    )
    private String comment;

    @NotNull(message = "Arrival date is required")
    @Schema(
            description = "Date and time the goods arrived / are expected to arrive",
            example = "2026-07-10T14:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime arrivalDate;

    @Schema(
            description = "Optional service fee added to order total",
            example = "50000.00"
    )
    private BigDecimal serviceFee;

    @Schema(description = "Document currency; null on create - UZS, on update - unchanged", example = "USD")
    private Currency currency;

    @DecimalMin(value = "0.0001", message = "Kurs musbat bo'lishi kerak")
    @Schema(description = "1 unit = rate UZS, set by the user allowed to create/edit receipts; "
            + "null - Central Bank rate for the arrival date", example = "11850")
    private BigDecimal exchangeRate;
}
