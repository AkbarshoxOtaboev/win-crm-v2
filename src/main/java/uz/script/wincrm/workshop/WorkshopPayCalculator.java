package uz.script.wincrm.workshop;

import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.workshop.enums.WorkshopPayBasis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Oyna qatoridan ish o'lchovi.
 * count WINDOW uchun allaqachon m²: (eni_sm × bo'yi_sm × dona) / 10 000.
 * Perimetr va charhlash: 2 × (eni + bo'yi) sm, donaga ko'paytiriladi va metrga o'tkaziladi.
 * 50×70 sm, 20 dona: kesish 7 m², perimetr va charhlash 48 m.
 */
public final class WorkshopPayCalculator {

    private static final BigDecimal CM_IN_M = BigDecimal.valueOf(100);
    private static final BigDecimal CM2_IN_M2 = BigDecimal.valueOf(10_000);
    private static final int MEASURE_SCALE = 4;

    private WorkshopPayCalculator() {
    }

    public static BigDecimal measure(List<SaleOrderItem> items, WorkshopPayBasis basis) {
        if (items == null || basis == null || basis == WorkshopPayBasis.PERCENT) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOrderItem item : items) {
            total = total.add(lineMeasure(item, basis));
        }
        return total.setScale(MEASURE_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal lineMeasure(SaleOrderItem item, WorkshopPayBasis basis) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        Goods goods = item.getGoods();
        if (goods == null || goods.getType() != Type.WINDOW) {
            return BigDecimal.ZERO;
        }
        BigDecimal width = item.getWidth();
        BigDecimal height = item.getHeight();
        BigDecimal squareMeters = item.getCount();
        if (width == null || height == null || squareMeters == null
                || width.signum() <= 0 || height.signum() <= 0 || squareMeters.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (basis == WorkshopPayBasis.SQUARE_METER) {
            return squareMeters;
        }
        BigDecimal pieces = squareMeters.multiply(CM2_IN_M2)
                .divide(width.multiply(height), MEASURE_SCALE, RoundingMode.HALF_UP);
        BigDecimal perimeterMeters = width.add(height)
                .multiply(BigDecimal.valueOf(2))
                .divide(CM_IN_M, MEASURE_SCALE, RoundingMode.HALF_UP)
                .multiply(pieces);
        return perimeterMeters;
    }
}
