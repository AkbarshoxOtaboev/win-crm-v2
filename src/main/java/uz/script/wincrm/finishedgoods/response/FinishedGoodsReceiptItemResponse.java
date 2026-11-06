package uz.script.wincrm.finishedgoods.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FinishedGoodsReceiptItemResponse {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private BigDecimal count;
    private BigDecimal unitCost;
}
