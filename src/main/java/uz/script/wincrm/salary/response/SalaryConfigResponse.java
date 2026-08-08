package uz.script.wincrm.salary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Salary Config Response", description = "Oylik konfiguratsiyasi ma'lumotlari")
public class SalaryConfigResponse {

    @Schema(description = "Konfiguratsiya ID si", example = "1")
    private Long id;

    @Schema(description = "Xodim ID si", example = "1")
    private Long userId;

    @Schema(description = "Xodimning to'liq ismi", example = "Ali Valiyev")
    private String userFullName;

    @Schema(description = "Oylik fiksa", example = "3000000.00")
    private BigDecimal baseSalary;

    @Schema(description = "Komissiya turi", example = "PERCENT")
    private CommissionType commissionType;

    @Schema(description = "Komissiya qiymati", example = "2")
    private BigDecimal commissionValue;

    @Schema(description = "Kuchga kirgan sana", example = "2026-08-01")
    private LocalDate effectiveFrom;

    @Schema(description = "Yopilgan sana (null bo'lsa amaldagi)", example = "null")
    private LocalDate effectiveTo;

    @Schema(description = "Holati", example = "ACTIVE")
    private Status status;

    @Schema(description = "Yaratilgan vaqt")
    private LocalDateTime createdAt;

    @Schema(description = "Yangilangan vaqt")
    private LocalDateTime updatedAt;

    @Schema(description = "Yaratgan foydalanuvchi", example = "admin")
    private String createdUsername;
}
