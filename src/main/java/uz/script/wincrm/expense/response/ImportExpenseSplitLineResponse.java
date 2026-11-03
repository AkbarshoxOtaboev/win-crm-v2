package uz.script.wincrm.expense.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ImportExpenseSplitLineResponse {

    private Long itemId;
    private String goodsName;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal pieces;
    private BigDecimal qty;
    /** Shu xarajatdan shu pozitsiyaga yozilgan ulush. */
    private BigDecimal amount;
}
