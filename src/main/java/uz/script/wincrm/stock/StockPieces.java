package uz.script.wincrm.stock;

import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stock miqdorining yagona manbasi count (WINDOW uchun kv.m). Dona/list soni undan hisoblanadi,
 * shuning uchun ikki birlik o'rtasida yaxlitlash farqi to'planmaydi.
 */
public final class StockPieces {

    public static final int SCALE = 4;

    private StockPieces() {
    }

    /**
     * WINDOW: count / (eni × bo'yi / 10000); boshqa turlar: count.
     * WINDOW mahsulot o'lchami noma'lum bo'lsa null qaytadi.
     */
    public static BigDecimal derive(Goods goods, BigDecimal count) {
        if (count == null) {
            return null;
        }
        if (goods == null || goods.getType() != Type.WINDOW) {
            return count;
        }
        BigDecimal w = goods.getWidth();
        BigDecimal h = goods.getHeight();
        if (w == null || h == null || w.signum() <= 0 || h.signum() <= 0) {
            return null;
        }
        return count.multiply(BigDecimal.valueOf(10_000)).divide(w.multiply(h), SCALE, RoundingMode.HALF_UP);
    }
}
