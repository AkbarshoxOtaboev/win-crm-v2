package uz.script.wincrm.sale.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.sale.enums.SaleOrderImageType;
import uz.script.wincrm.utils.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Sale Order Image Response", description = "Sale order image information returned by the API")
public class SaleOrderImageResponse {

    @Schema(description = "Unique image identifier", example = "1")
    private Long id;

    @Schema(description = "Sale order ID", example = "1")
    private Long saleOrderId;

    @Schema(description = "Saqlangan fayl nomi (yuklab olish uchun)", example = "3f2c9a1e-....jpg")
    private String fileName;

    @Schema(description = "Asl fayl nomi", example = "obyekt-rasmi.jpg")
    private String originalFileName;

    @Schema(description = "Fayl yuklab olish URL manzili", example = "/api/files/3f2c9a1e-....jpg")
    private String downloadUrl;

    @Schema(description = "MIME turi", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Fayl hajmi (bayt)", example = "204800")
    private Long size;

    @Schema(description = "Rasm turi", example = "OBJECT")
    private SaleOrderImageType imageType;

    @Schema(description = "Rasm holati", example = "ACTIVE")
    private Status status;

    @Schema(description = "Yaratilgan sana", example = "2026-08-08T09:30:15")
    private LocalDateTime createdAt;

    @Schema(description = "Yuklagan foydalanuvchi ID", example = "1")
    private Long createdBy;
}