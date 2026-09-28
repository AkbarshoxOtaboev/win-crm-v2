package uz.script.wincrm.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Transport Worker DTO")
public class TransportWorkerDTO {

    @NotBlank(message = "Full name is required")
    @Schema(example = "Karimov Anvar", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Schema(example = "+998901234567")
    private String phone;

    private String note;
}
