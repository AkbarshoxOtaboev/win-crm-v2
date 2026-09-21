package uz.script.wincrm.production.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CompleteProductionDTO")
public class CompleteProductionDTO {
    private String note;
}
