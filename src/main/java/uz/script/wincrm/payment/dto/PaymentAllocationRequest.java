package uz.script.wincrm.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "Taqsimlanmagan to'lovlarni buyurtmalarga qo'lda taqsimlash (akt sverka)")
public class PaymentAllocationRequest {

    @NotNull(message = "Mijoz tanlanmagan")
    @Schema(description = "Mijoz identifikatori", example = "1")
    private Long clientId;

    @NotEmpty(message = "Taqsimlanadigan to'lovlarni belgilang")
    @Schema(description = "Taqsimlanadigan (buyurtmasiz) to'lovlar", example = "[10, 11]")
    private List<Long> paymentIds;

    @Valid
    @NotEmpty(message = "Buyurtmalarni belgilang")
    @Schema(description = "Buyurtma va unga yoziladigan summa")
    private List<Allocation> allocations;

    @Getter
    @Setter
    public static class Allocation {

        @NotNull(message = "Buyurtma tanlanmagan")
        @Schema(description = "Buyurtma identifikatori", example = "5")
        private Long saleOrderId;

        @NotNull(message = "Summa kiritilmagan")
        @DecimalMin(value = "0.01", message = "Summa 0 dan katta bo'lishi kerak")
        @Schema(description = "Buyurtmaga yoziladigan summa", example = "150000")
        private BigDecimal amount;
    }
}
