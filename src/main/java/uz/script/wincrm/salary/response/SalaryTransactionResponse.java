package uz.script.wincrm.salary.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.salary.enums.SalaryEntryType;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Salary Transaction Response", description = "Oylik ledger yozuvi (komissiya yoki tuzatish)")
public class SalaryTransactionResponse {

    @Schema(description = "Yozuv ID si", example = "1")
    private Long id;

    @Schema(description = "Xodim ID si", example = "1")
    private Long userId;

    @Schema(description = "Manba buyurtma ID si (bo'lsa)", example = "42")
    private Long saleOrderId;

    @Schema(description = "Yozuv turi", example = "COMMISSION")
    private SalaryEntryType entryType;

    @Schema(description = "Summa (musbat magnitud)", example = "20000.00")
    private BigDecimal amount;

    @Schema(description = "Komissiya turi snapshoti", example = "PERCENT")
    private CommissionType commissionTypeSnapshot;

    @Schema(description = "Stavka snapshoti", example = "2")
    private BigDecimal rateSnapshot;

    @Schema(description = "Baza summa snapshoti", example = "1000000.00")
    private BigDecimal baseAmountSnapshot;

    @Schema(description = "Tan olingan vaqt", example = "2026-08-08T10:15:30")
    private LocalDateTime earnedAt;

    @Schema(description = "Davr yili", example = "2026")
    private Integer periodYear;

    @Schema(description = "Davr oyi", example = "8")
    private Integer periodMonth;

    @Schema(description = "Izoh")
    private String comment;

    @Schema(description = "Holati", example = "ACTIVE")
    private Status status;

    @Schema(description = "Yaratilgan vaqt")
    private LocalDateTime createdAt;

    @Schema(description = "Yaratgan foydalanuvchi", example = "admin")
    private String createdUsername;
}
