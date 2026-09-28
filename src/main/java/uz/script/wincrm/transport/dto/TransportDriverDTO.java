package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

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

    private String note;
}
