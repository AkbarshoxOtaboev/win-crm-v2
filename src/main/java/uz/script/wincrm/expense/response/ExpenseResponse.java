package uz.script.wincrm.expense.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Expense Response", description = "Expense information returned by the API")
public class ExpenseResponse {

    @Schema(description = "Unique expense identifier", example = "1")
    private Long id;

    @Schema(description = "Expense category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Expense category name", example = "Ijara")
    private String categoryName;

    @Schema(description = "Expense amount", example = "150000.00")
    private BigDecimal amount;

    @Schema(description = "amount valyutasi", example = "UZS")
    private Currency currency;

    @Schema(description = "Xorijiy valyutadagi xarajat kursi (1 birlik = N so'm)", example = "12850")
    private BigDecimal exchangeRate;

    @Schema(description = "Pul chiqqan kassa ID", example = "1")
    private Long paymentTypeId;

    @Schema(description = "Pul chiqqan kassa nomi", example = "Naqd")
    private String paymentTypeName;

    @Schema(description = "Expense date", example = "2026-07-09")
    private LocalDate expenseDate;

    @Schema(description = "Expense description")
    private String description;

    @Schema(description = "Current status", example = "ACTIVE")
    private Status status;

    @Schema(description = "Date and time when the expense was created", example = "2026-07-09T09:30:15")
    private LocalDateTime createdAt;

    @Schema(description = "Date and time when the expense was last updated", example = "2026-07-09T11:45:30")
    private LocalDateTime updatedAt;

    @Schema(description = "ID of the user who created the expense", example = "1")
    private Long createdBy;

    @Schema(description = "Supplier payment that generated this expense (read-only expense if set)", example = "5")
    private Long supplierPaymentId;
}