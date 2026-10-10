package uz.script.wincrm.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Payment DTO", description = "Payment data transfer object for creating and updating")
public class PaymentDTO {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Sale order ID (optional — leave empty for a general/advance payment not tied to a specific order)", example = "1")
    private Long saleOrderId;

    @NotNull(message = "Payment type ID is required")
    @Schema(description = "Payment type ID", example = "1")
    private Long paymentTypeId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Schema(description = "Kassaga tushgan summa - to'lov turi valyutasida", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal paymentAmount;

    @Schema(description = "Qaysi valyutadagi qarz yopiladi. Buyurtma berilsa - buyurtma valyutasi; bo'sh bo'lsa - kassa valyutasi. "
            + "Valyutalar farq qilsa to'lov kunidagi Markaziy bank kursi qo'llanadi.", example = "USD")
    private Currency debtCurrency;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than 0")
    @Schema(description = "Qo'lda kiritilgan kurs: 1 birlik xorijiy valyuta = shuncha so'm. Bo'sh bo'lsa - "
            + "to'lov kunidagi Markaziy bank kursi. Valyutalar bir xil bo'lsa e'tiborga olinmaydi.", example = "12850")
    private BigDecimal exchangeRate;

    @NotNull(message = "Payment date is required")
    @Schema(
            description = "Payment date and time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime paymentDate;

    @Schema(description = "Comment/Notes about the payment")
    private String comment;
}