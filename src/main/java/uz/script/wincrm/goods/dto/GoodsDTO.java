package uz.script.wincrm.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.goods.enums.Type;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(name = "Goods DTO", description = "Goods creation and update request")
public class GoodsDTO {

    @NotBlank(message = "Name is required")
    @Schema(
            description = "Goods name",
            example = "Coca-Cola 1.5L",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "Goods group identifier (SERVICE turida bo'sh bo'lsa — \"Xizmatlar\" guruhi)",
            example = "1"
    )
    private Long goodsGroupId;

    @Schema(
            description = "Unit type identifier (SERVICE turida bo'sh bo'lsa — \"xizmat\" birligi)",
            example = "1"
    )
    private Long unitTypeId;

    @NotNull(message = "Type is required")
    @Schema(
            description = "Goods type",
            example = "PRODUCT",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Type type;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price cost must not be negative")
    @Schema(
            description = "Cost price (optional: set by warehouse arrivals; empty on create - 0, on update - unchanged)",
            example = "8500.00"
    )
    private BigDecimal priceCost;

    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price must not be negative")
    @Schema(
            description = "Selling price (optional: set by warehouse arrivals; empty on create - 0, on update - unchanged)",
            example = "12000.00"
    )
    private BigDecimal priceSelling;

    @Schema(description = "Sotish narxi valyutasi (bo'sh — UZS). Tannarx doim so'mda.", example = "USD")
    private Currency priceCurrency;

    @Schema(
            description = "Goods barcode",
            example = "4870204012345"
    )
    private String barcode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maksimal chegirma manfiy bo'lishi mumkin emas")
    @DecimalMax(value = "100.0", inclusive = true, message = "Maksimal chegirma 100% dan oshmasligi kerak")
    @Schema(description = "Sotuvchi bera oladigan maksimal chegirma, % (yaratishda bo'sh — cheklanmagan, tahrirda bo'sh — o'zgarmaydi)", example = "5")
    private BigDecimal maxDiscountPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimal qoldiq manfiy bo'lishi mumkin emas")
    @Schema(description = "Minimal qoldiq (ixtiyoriy). PRODUCT — birlikda; WINDOW — kv.m. Bo'sh — chegaralanmagan.", example = "10")
    private BigDecimal minQuantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimal dona manfiy bo'lishi mumkin emas")
    @Schema(description = "WINDOW uchun minimal qoldiq donada (ixtiyoriy). Bo'sh — chegara yo'q.", example = "5")
    private BigDecimal minPieceQuantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than 0")
    @Schema(description = "Default window width in centimeters (WINDOW type)", example = "120")
    private BigDecimal width;

    @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than 0")
    @Schema(description = "Default window height in centimeters (WINDOW type)", example = "150")
    private BigDecimal height;

    @DecimalMin(value = "0.0", inclusive = false, message = "Thickness must be greater than 0")
    @Schema(description = "Window thickness in millimeters (WINDOW type)", example = "4")
    private BigDecimal thickness;

    @DecimalMin(value = "0.0", inclusive = false, message = "Weight per m2 must be greater than 0")
    @Schema(description = "Weight of 1 m2 in kilograms (WINDOW type)", example = "10")
    private BigDecimal weightPerSqm;

    @Schema(
            description = "Goods photo file. Optional on create and update.",
            type = "string",
            format = "binary"
    )
    private MultipartFile photo;
}