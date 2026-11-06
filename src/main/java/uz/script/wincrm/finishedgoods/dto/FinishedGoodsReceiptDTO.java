package uz.script.wincrm.finishedgoods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FinishedGoodsReceiptDTO {

    @NotNull
    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "1")
    private Long workshopId;

    @Schema(example = "Sexdan tayyor mahsulot")
    private String comment;

    @NotNull
    private LocalDateTime documentDate;

    @NotEmpty
    @Valid
    private List<FinishedGoodsReceiptItemDTO> items;
}
