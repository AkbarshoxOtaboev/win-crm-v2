package uz.script.wincrm.stock.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Stock Response", description = "Stock information returned by the API")
public class StockResponse {

    @Schema(description = "Unique stock identifier", example = "1")
    private Long id;

    @Schema(description = "Goods identifier", example = "1")
    private Long goodsId;

    @Schema(description = "Goods name", example = "Coca-Cola 1.5L")
    private String goodsName;

    @Schema(description = "Goods type", example = "WINDOW", implementation = Type.class)
    private Type goodsType;

    @Schema(description = "Unit type name", example = "dona")
    private String unitTypeName;

    @Schema(description = "Window width in cm (WINDOW)", example = "260")
    private BigDecimal width;

    @Schema(description = "Window height in cm (WINDOW)", example = "180")
    private BigDecimal height;

    @Schema(description = "Warehouse identifier", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Central Warehouse")
    private String warehouseName;

    @Schema(description = "Current quantity in stock (kv.m for WINDOW, otherwise unit count)", example = "23.4")
    private BigDecimal count;

    @Schema(description = "Quantity in pieces (dona). For WINDOW derived from kv.m and dimensions.", example = "5")
    private BigDecimal pieceCount;

    @Schema(description = "Quantity in square meters (only for WINDOW)", example = "23.4")
    private BigDecimal kvm;

    @Schema(description = "Current stock status", example = "ACTIVE", implementation = Status.class)
    private Status status;

    @Schema(description = "Date and time when the stock record was created", example = "2026-07-04T09:30:15")
    private LocalDateTime createdAt;

    @Schema(description = "Date and time when the stock record was last updated", example = "2026-07-04T11:45:30")
    private LocalDateTime updatedAt;

    @Schema(description = "Username of the user who created the stock record", example = "admin")
    private String createdUsername;
}