package uz.script.wincrm.salary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.script.wincrm.salary.enums.SalaryEntryType;

import java.math.BigDecimal;

/**
 * Qo'lda kiritiladigan oylik tuzatishlar uchun: BONUS, DEDUCTION, ADVANCE.
 * COMMISSION / COMMISSION_REVERSAL faqat tizim tomonidan (to'lov hodisasida) yoziladi,
 * bu DTO orqali kiritib bo'lmaydi (service tekshiradi).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Salary Adjustment DTO", description = "Qo'lda oylik tuzatish (bonus/deduction/advance) kiritish")
public class SalaryAdjustmentDTO {

    @NotNull(message = "User ID is required")
    @Schema(description = "Xodim ID si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "Entry type is required")
    @Schema(description = "BONUS, DEDUCTION yoki ADVANCE", example = "BONUS", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryEntryType entryType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Summa (musbat magnitud)", example = "500000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Qaysi davrga (yil). Bo'sh bo'lsa joriy yil olinadi", example = "2026")
    private Integer periodYear;

    @Schema(description = "Qaysi davrga (oy 1..12). Bo'sh bo'lsa joriy oy olinadi", example = "8")
    private Integer periodMonth;

    @Schema(description = "Izoh", example = "Oylik reja bajarilgani uchun mukofot")
    private String comment;
}
