package uz.script.wincrm.finishedgoods.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MaterialIssueItemResponse {
    private Long id;
    private Long goodsId;
    private String goodsName;
    private String goodsType;
    private BigDecimal count;
    private BigDecimal pieceCount;
}
