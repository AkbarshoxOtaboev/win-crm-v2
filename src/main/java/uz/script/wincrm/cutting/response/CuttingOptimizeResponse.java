package uz.script.wincrm.cutting.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CuttingOptimizeResponse {
    private Long saleOrderId;
    private Long warehouseId;
    private String warehouseName;
    private boolean optimized;
    private List<Line> lines;
    private List<RemnantStock> remnants;

    @Getter
    @Builder
    public static class Line {
        private Long itemId;
        private Long goodsId;
        private String goodsName;
        private BigDecimal width;
        private BigDecimal height;
        private Integer count;
        private BigDecimal kerfMm;
        private Boolean allowRotate;
        private Integer sheetQuantity;
        private List<SavedPick> picks;
    }

    @Getter
    @Builder
    public static class SavedPick {
        private Long remnantId;
        private Integer quantity;
        private BigDecimal width;
        private BigDecimal height;
    }

    @Getter
    @Builder
    public static class RemnantStock {
        private Long id;
        private Long goodsId;
        private String goodsName;
        private BigDecimal width;
        private BigDecimal height;
        private Integer pieceCount;
    }
}
