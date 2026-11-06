package uz.script.wincrm.cutting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CuttingOptimizeRequest {

    /** 0 yoki 3. Bo'sh bo'lsa qatordagi qiymat qoladi. */
    private BigDecimal kerfMm;

    /** Chegaradan qoladigan foydali qoldiq, sm. Masalan 14 yoki 20. */
    @DecimalMin("0")
    private BigDecimal minRemnantCm;

    private Boolean allowRotate;

    @Valid
    @NotNull
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    public static class Line {
        @NotNull
        private Long saleOrderItemId;

        /** Eski mijozlar: bitta list turi — qatorning o'z mahsuloti. */
        @Min(0)
        private Integer sheetQuantity;

        @Valid
        private List<SheetPick> sheets = new ArrayList<>();

        @Valid
        private List<RemnantPick> remnants = new ArrayList<>();

        /** Shu buyurtmaning yangi listdan chiqadigan qoldig'i. Saqlashda yaratiladi. */
        @Valid
        private List<PlannedPick> planned = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class SheetPick {
        @NotNull
        private Long goodsId;

        @NotNull
        @Min(0)
        private Integer quantity;
    }

    @Getter
    @Setter
    public static class RemnantPick {
        @NotNull
        private Long remnantId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Getter
    @Setter
    public static class PlannedPick {
        private Long goodsId;

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal width;

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal height;

        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
