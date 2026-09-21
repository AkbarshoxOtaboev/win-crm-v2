package uz.script.wincrm.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SendToProductionDTO")
public class SendToProductionDTO {

    @NotNull
    private Long saleOrderId;

    @NotNull
    private Long workshopId;

    private String note;
}
