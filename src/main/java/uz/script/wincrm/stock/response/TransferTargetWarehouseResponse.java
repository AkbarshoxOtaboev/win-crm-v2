package uz.script.wincrm.stock.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "Transfer Target Warehouse", description = "Transfer uchun tanlanadigan ombor (barcha filiallar bo'yicha)")
public class TransferTargetWarehouseResponse {

    @Schema(description = "Ombor identifikatori", example = "3")
    private Long id;

    @Schema(description = "Ombor nomi", example = "Samarqand ombori")
    private String name;

    @Schema(description = "Filial identifikatori", example = "2")
    private Long filialId;

    @Schema(description = "Filial nomi", example = "Samarqand")
    private String filialName;
}
