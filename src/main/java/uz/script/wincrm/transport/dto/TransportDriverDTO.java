package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(name = "Transport Driver DTO")
public class TransportDriverDTO {

    @NotBlank(message = "Full name is required")
    @Schema(example = "Aliyev Vali", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Schema(example = "+998901234567")
    private String phone;

    @Schema(example = "Damas")
    private String carModel;

    @Schema(example = "01 A 123 BC")
    private String carNumber;

    @NotNull(message = "Max vehicle weight is required")
    @DecimalMin(value = "0.01", message = "Max vehicle weight must be positive")
    @Schema(description = "Max vehicle load capacity in kg", example = "800", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal maxWeightKg;

    private String note;
}
