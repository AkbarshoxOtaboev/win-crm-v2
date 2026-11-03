package uz.script.wincrm.stock;

import java.math.BigDecimal;

/**
 * Ombordagi qoldiq uchun yetkazib beruvchi pozitsiyalaridan yig‘ilgan narx.
 * unitSom — kelgan + naqd + import taqsimoti, so‘mda (1 birlik).
 * foreignUnit — shu yig‘indi hujjat valyutasida; so‘m kirimda bo‘sh.
 * importUnitSom — faqat import ulushi, so‘mda (1 birlik).
 */
public record StockArrival(
        BigDecimal unitSom,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal foreignUnit,
        BigDecimal selling,
        String sellingCurrency,
        BigDecimal sellingSom,
        BigDecimal importUnitSom
) {
    public StockArrival(
            BigDecimal unitSom,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal foreignUnit,
            BigDecimal selling,
            String sellingCurrency,
            BigDecimal sellingSom
    ) {
        this(unitSom, currency, exchangeRate, foreignUnit, selling, sellingCurrency, sellingSom, null);
    }
}
