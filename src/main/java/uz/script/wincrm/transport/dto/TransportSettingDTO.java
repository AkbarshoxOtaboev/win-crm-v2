package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(name = "Transport Setting DTO")
public class TransportSettingDTO {

    @NotNull(message = "Percent is required")
    @DecimalMin(value = "0", message = "Percent must be between 0 and 100")
    @DecimalMax(value = "100", message = "Percent must be between 0 and 100")
    @Schema(example = "3.00")
    private BigDecimal workerSalaryPercent;
}
