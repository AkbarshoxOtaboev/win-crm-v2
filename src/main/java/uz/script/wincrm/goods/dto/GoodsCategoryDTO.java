package uz.script.wincrm.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Mahsulot kategoriyasi")
public class GoodsCategoryDTO {

    @NotBlank(message = "Kategoriya nomi bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Kategoriya nomi 100 ta belgidan oshmasligi kerak")
    @Schema(description = "Kategoriya nomi", example = "3 mm oq", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
