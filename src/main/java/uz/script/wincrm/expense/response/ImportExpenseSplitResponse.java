package uz.script.wincrm.expense.response;

import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.ImportExpenseKind;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ImportExpenseSplitResponse {

    private Long expenseId;
    private Long warehouseOrderId;
    private String supplierName;
    private ImportExpenseKind kind;
    /** Shu kirim + tur + valyutadagi barcha to'lovlar jamisi. */
    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private Integer paymentCount;
    private Currency currency;
    private List<ImportExpenseSplitLineResponse> lines;
}
