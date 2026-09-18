package uz.script.wincrm.filial;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Filial create/update payload")
public class FilialDTO {
    @NotBlank(message = "Filial nomi bo'sh bo'lmasin")
    private String name;
    private String address;
    private String phone;
    private Long directorId;
}
