package uz.script.wincrm.expense.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Import xarajati formasidagi pozitsiya tanlovi. */
@Getter
@Builder
public class ImportExpenseItemOption {

    private Long id;
    private String goodsName;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal pieceCount;
    private BigDecimal count;
}
