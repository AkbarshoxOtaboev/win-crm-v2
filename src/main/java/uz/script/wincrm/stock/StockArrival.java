package uz.script.wincrm.stock;

import java.math.BigDecimal;

/**
 * Ombordagi qoldiq uchun yetkazib beruvchi pozitsiyalaridan yig‘ilgan narx.
 * unitSom — tannarx va naqd narx yig‘indisi, so‘mda (1 birlik).
 * foreignUnit — shu yig‘indi hujjat valyutasida; so‘m kirimda bo‘sh.
 */
public record StockArrival(
        BigDecimal unitSom,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal foreignUnit,
        BigDecimal selling,
        String sellingCurrency,
        BigDecimal sellingSom
) {
}
