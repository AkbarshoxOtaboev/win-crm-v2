package uz.script.wincrm.cutting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(name = "Cutting Calculate Request", description = "O'lchamlar santimetrda")
public class CuttingCalculateRequest {

    @Schema(description = "Oyna mahsuloti: list o'lchami shundan olinadi (sheetWidth/sheetHeight berilmasa)")
    private Long goodsId;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal sheetWidth;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal sheetHeight;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal pieceWidth;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal pieceHeight;

    @NotNull
    @Min(1)
    @Max(100_000)
    private Integer quantity;

    @Schema(description = "Kesuv chizig'i qalinligi, mm; berilmasa sozlamadagi qiymat")
    @DecimalMin("0")
    @DecimalMax("50")
    private BigDecimal kerfMm;

    @Schema(description = "Bo'lakni 90° burish mumkinmi; berilmasa - mumkin")
    private Boolean allowRotate;

    @Schema(description = "Foydali qoldiqning minimal tomoni, sm; en va bo'y berilmasa shu qiymat ikkala tomonga qo'llanadi")
    @DecimalMin("0")
    private BigDecimal minRemnantCm;

    @Schema(description = "Qoldiq shu enidan katta bo'lsa butun saqlanadi va qolgan bo'lak boshqa listdan olinadi, sm")
    @DecimalMin("0")
    private BigDecimal minRemnantWidthCm;

    @Schema(description = "Qoldiq shu bo'yidan katta bo'lsa butun saqlanadi va qolgan bo'lak boshqa listdan olinadi, sm")
    @DecimalMin("0")
    private BigDecimal minRemnantHeightCm;

    @Schema(description = "To'liq listdagi chiqindi shu foizdan oshmasin")
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal maxScrapPercent;

    @Schema(description = "Yaroqsiz tasma eni, sm")
    @DecimalMin("0")
    private BigDecimal wasteWidthCm;

    @Schema(description = "Yaroqsiz tasma bo'yi, sm")
    @DecimalMin("0")
    private BigDecimal wasteHeightCm;
}
