package uz.script.wincrm.dashboard.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.goods.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(name = "Profit Report Response",
        description = "Sotilgan tovarlardan foyda: tushum (chegirmadan keyin, yetkazishsiz) - tannarx")
public class ProfitReportResponse {

    @Schema(description = "Summalar valyutasi", example = "UZS")
    private Currency currency;

    @Schema(description = "Tovarlardan tushum: chegirmadan keyingi buyurtma summasi, yetkazib berish haqisiz")
    private BigDecimal revenue;

    @Schema(description = "Sotilgan tovarlar tannarxi (sotuv paytidagi ombor o'rtacha narxi)")
    private BigDecimal cost;

    @Schema(description = "revenue - cost")
    private BigDecimal profit;

    @Schema(description = "profit / revenue * 100")
    private BigDecimal marginPercent;

    @Schema(description = "Berilgan chegirmalar jami")
    private BigDecimal discount;

    @Schema(description = "Yetkazib berish haqi (foydaga kirmaydi)")
    private BigDecimal deliveryFee;

    private long orderCount;

    @Schema(description = "Pozitsiyasiz buyurtmalar soni: tushumi bor, tannarxi 0 (mahsulotlar jadvaliga kirmaydi)")
    private long itemlessOrderCount;

    private List<Day> days;

    private List<GoodsRow> goods;

    private List<SellerRow> sellers;

    @Getter
    @Setter
    @Builder
    @Schema(name = "Profit Report Day")
    public static class Day {
        private LocalDate date;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
    }

    @Getter
    @Setter
    @Builder
    @Schema(name = "Profit Report Goods Row")
    public static class GoodsRow {
        private Long goodsId;
        private String goodsName;
        private Type goodsType;
        private String unitName;
        @Schema(description = "Sotilgan miqdor (WINDOW: kv.m)")
        private BigDecimal count;
        @Schema(description = "Buyurtma chegirmasi pozitsiyalarga summasi ulushiga qarab taqsimlangan tushum")
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal marginPercent;
    }

    @Getter
    @Setter
    @Builder
    @Schema(name = "Profit Report Seller Row")
    public static class SellerRow {
        private Long userId;
        private String fullName;
        private long orderCount;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal marginPercent;
    }
}
