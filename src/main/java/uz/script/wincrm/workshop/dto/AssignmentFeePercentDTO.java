package uz.script.wincrm.workshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(name = "Assignment fee percent DTO")
public class AssignmentFeePercentDTO {

    @NotNull
    @DecimalMin(value = "0.0", message = "Fee percent must be >= 0")
    @DecimalMax(value = "100.0", message = "Fee percent must be <= 100")
    @Schema(description = "Fee percent of sale order total", example = "5.00")
    private BigDecimal feePercent;
}
