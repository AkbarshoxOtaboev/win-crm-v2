package uz.script.wincrm.finishedgoods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialIssueItemDTO {

    @NotNull
    @Schema(example = "1")
    private Long goodsId;

    @NotNull
    @DecimalMin(value = "0.0001")
    @Schema(example = "10")
    private BigDecimal count;

    @Schema(description = "WINDOW uchun dona (ixtiyoriy)", example = "2")
    private BigDecimal pieceCount;
}
