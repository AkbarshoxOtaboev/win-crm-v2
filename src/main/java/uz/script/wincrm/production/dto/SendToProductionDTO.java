package uz.script.wincrm.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "SendToProductionDTO")
public class SendToProductionDTO {

    @NotNull
    private Long saleOrderId;

    @Schema(description = "Birinchi sex (workshopIds berilmasa ishlatiladi)")
    private Long workshopId;

    @Schema(description = "Sexlar ketma-ketligi: birinchisi darhol tayinlanadi, qolganlari navbat bilan", example = "[1, 3]")
    private List<Long> workshopIds;

    private String note;
}
