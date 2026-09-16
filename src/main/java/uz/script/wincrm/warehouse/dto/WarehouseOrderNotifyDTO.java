package uz.script.wincrm.warehouse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "Warehouse Order Notify DTO", description = "SMS yoki Telegram orqali yetkazuvchiga xabar yuborish")
public class WarehouseOrderNotifyDTO {

    @NotBlank(message = "Xabar matni bo'sh bo'lmasligi kerak")
    @Size(max = 4096, message = "Xabar matni 4096 belgidan oshmasligi kerak")
    @Schema(description = "Yuboriladigan xabar matni", example = "Kirim buyurtmasi #12 haqida ma'lumot")
    private String message;
}
