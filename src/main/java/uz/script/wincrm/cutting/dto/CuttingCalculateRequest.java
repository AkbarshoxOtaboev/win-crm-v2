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

    @Schema(description = "Foydali qoldiqning minimal tomoni, sm; berilmasa sozlamadagi qiymat")
    @DecimalMin("0")
    private BigDecimal minRemnantCm;
}
