package uz.script.wincrm.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "RedirectProductionDTO")
public class RedirectProductionDTO {

    @NotNull
    private Long nextWorkshopId;

    private String note;
}
