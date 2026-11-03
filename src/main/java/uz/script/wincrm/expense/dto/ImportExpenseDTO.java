package uz.script.wincrm.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.expense.ImportExpenseKind;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Import Expense DTO", description = "Boj yoki yo'l haqi. Kirim buyurtma va pozitsiyaga bog'lanadi")
public class ImportExpenseDTO {

    @NotNull(message = "Kirim buyurtma tanlanishi shart")
    @Schema(description = "Kirim buyurtma ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseOrderId;

    @Schema(description = "Pozitsiya ID. Kirimda bitta pozitsiya bo'lsa bo'sh qoldirish mumkin", example = "10")
    private Long warehouseOrderItemId;

    @NotNull(message = "Xarajat turi tanlanishi shart")
    @Schema(description = "ROAD — yo'l haqi, DUTY — boj", example = "ROAD", requiredMode = Schema.RequiredMode.REQUIRED)
    private ImportExpenseKind kind;

    @NotNull(message = "Summa kiritilishi shart")
    @DecimalMin(value = "0.01", message = "Summa 0 dan katta bo'lishi kerak")
    @Schema(description = "Summa, kassa valyutasida", example = "800.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotNull(message = "Sana kiritilishi shart")
    @Schema(description = "Xarajat sanasi", example = "2026-11-03", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate expenseDate;

    @Size(max = 500, message = "Izoh 500 belgidan oshmasin")
    @Schema(description = "Izoh")
    private String description;

    @NotNull(message = "Kassa tanlanishi shart")
    @Schema(description = "Pul chiqadigan kassa ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentTypeId;

    @Schema(description = "OPERATIONAL yoki VAULT. Bo'sh bo'lsa roldan olinadi", example = "OPERATIONAL")
    private CashSource cashSource;
}
