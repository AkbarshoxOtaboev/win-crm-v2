package uz.script.wincrm.salary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.script.wincrm.salary.enums.CommissionType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Salary Config DTO", description = "Xodim uchun oylik fiksa va komissiya konfiguratsiyasini yaratish/yangilash")
public class SalaryConfigDTO {

    @NotNull(message = "User ID is required")
    @Schema(description = "Xodim (sotuvchi) ID si", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "Base salary is required")
    @DecimalMin(value = "0.0", message = "Base salary cannot be negative")
    @Schema(description = "Oylik fiksa", example = "3000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal baseSalary;

    @NotNull(message = "Commission type is required")
    @Schema(description = "Komissiya turi: PERCENT yoki FIXED", example = "PERCENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private CommissionType commissionType;

    @NotNull(message = "Commission value is required")
    @DecimalMin(value = "0.0", message = "Commission value cannot be negative")
    @Schema(description = "PERCENT bo'lsa foiz (2 => 2%), FIXED bo'lsa har buyurtma uchun summa", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal commissionValue;

    @NotNull(message = "effectiveFrom is required")
    @Schema(description = "Konfiguratsiya kuchga kiradigan sana", example = "2026-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveFrom;
}
