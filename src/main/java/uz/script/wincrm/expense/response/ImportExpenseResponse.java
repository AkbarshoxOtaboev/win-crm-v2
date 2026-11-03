package uz.script.wincrm.expense.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.ImportExpenseKind;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Import Expense Response")
public class ImportExpenseResponse {

    private Long id;
    private Long warehouseOrderId;
    private Long warehouseOrderItemId;
    private String supplierName;
    private LocalDateTime orderArrivalDate;
    private ImportExpenseKind kind;
    private BigDecimal amount;
    /** Shu kirim + tur + valyutadagi barcha to'lovlar jamisi. */
    private BigDecimal groupAmount;
    /** Shu guruh bo'yicha pozitsiyalarga taqsimlangan jami. */
    private BigDecimal allocatedAmount;
    /** Shu guruhdagi to'lovlar soni. */
    private Integer paymentCount;
    private Currency currency;
    private BigDecimal exchangeRate;
    private Long paymentTypeId;
    private String paymentTypeName;
    private CashSource cashSource;
    private LocalDate expenseDate;
    private String description;
    private String positionGoodsName;
    private BigDecimal positionWidth;
    private BigDecimal positionHeight;
    private BigDecimal positionPieces;
    private BigDecimal positionQty;
    private Status status;
    private LocalDateTime createdAt;
}
