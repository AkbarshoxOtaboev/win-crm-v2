package uz.script.wincrm.workshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Workshop DTO", description = "Workshop creation and update request")
public class WorkshopDTO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Workshop name", example = "Oyna kesish", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Optional description")
    private String description;

    @Schema(description = "Manager user id")
    private Long managerId;

    @Schema(description = "Default fee percent of sale order total (0-100)", example = "5.00")
    private java.math.BigDecimal feePercent;
}
