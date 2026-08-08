package uz.script.wincrm.salary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Bir xodimning bitta oy uchun yig'ma oylik hisob-kitobi (payslip).
 * SalaryTransaction ledger'idan agregatsiya + amaldagi SalaryConfig'dan fiksa asosida hisoblanadi.
 *
 * netSalary = baseSalary + totalCommission - totalCommissionReversal
 *             + totalBonus - totalDeduction - totalAdvance
 */
@Getter
@Setter
@Builder
@Schema(name = "Salary Slip Response", description = "Xodimning oylik yig'ma hisob-kitobi")
public class SalarySlipResponse {

    @Schema(description = "Xodim ID si", example = "1")
    private Long userId;

    @Schema(description = "Xodimning to'liq ismi", example = "Ali Valiyev")
    private String userFullName;

    @Schema(description = "Davr yili", example = "2026")
    private Integer periodYear;

    @Schema(description = "Davr oyi (1..12)", example = "8")
    private Integer periodMonth;

    @Schema(description = "Oylik fiksa (davr uchun amaldagi konfiguratsiyadan)", example = "3000000.00")
    private BigDecimal baseSalary;

    @Schema(description = "Davr ichida hisoblangan jami komissiya", example = "400000.00")
    private BigDecimal totalCommission;

    @Schema(description = "Davr ichida qaytarilgan komissiya", example = "0.00")
    private BigDecimal totalCommissionReversal;

    @Schema(description = "Jami bonus", example = "500000.00")
    private BigDecimal totalBonus;

    @Schema(description = "Jami ushlanmalar (jarima/kamomad)", example = "0.00")
    private BigDecimal totalDeduction;

    @Schema(description = "Jami avans", example = "1000000.00")
    private BigDecimal totalAdvance;

    @Schema(description = "Yakuniy to'lanadigan summa", example = "2900000.00")
    private BigDecimal netSalary;
}
