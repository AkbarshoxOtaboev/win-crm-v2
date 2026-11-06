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
    private BigDecimal kerfMm;
    private BigDecimal minRemnantCm;
    private BigDecimal minRemnantWidthCm;
    private BigDecimal minRemnantHeightCm;
    /** REMNANT yoki SHEET. */
    private String source;
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
        private BigDecimal minRemnantCm;
        private Boolean allowRotate;
        private Integer sheetQuantity;
        private List<Long> candidateGoodsIds;
        private List<SheetOption> candidates;
        private List<SheetUse> sheetUses;
        private List<SavedPick> picks;
    }

    @Getter
    @Builder
    public static class SheetOption {
        private Long goodsId;
        private String goodsName;
        private BigDecimal width;
        private BigDecimal height;
    }

    @Getter
    @Builder
    public static class SheetUse {
        private Long goodsId;
        private String goodsName;
        private BigDecimal width;
        private BigDecimal height;
        private Integer quantity;
    }

    @Getter
    @Builder
    public static class SavedPick {
        private Long remnantId;
        private Long goodsId;
        private Integer quantity;
        private BigDecimal width;
        private BigDecimal height;
        /** Omborda hali yo'q: shu buyurtma kesishidan chiqadigan qoldiq. */
        private boolean planned;
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
        private String warehouseName;
        private Long sourceSaleOrderId;
    }
}
