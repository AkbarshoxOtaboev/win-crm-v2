package uz.script.wincrm.currency;

import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Kurs bilan o'girish: {@code rate} - 1 birlik xorijiy valyuta necha so'm. Natija 2 xonagacha HALF_UP.
 * Faqat so'm va bitta xorijiy valyuta orasida o'giriladi.
 */
public final class CurrencyMath {

    /** Valyutadagi yaxlitlash qoldig'i: shundan kichik qarz to'langan hisoblanadi. */
    public static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private CurrencyMath() {
    }

    public static Currency orBase(Currency currency) {
        return currency != null ? currency : Currency.BASE;
    }

    public static BigDecimal convert(BigDecimal amount, Currency from, Currency to, BigDecimal rate) {
        if (amount == null) {
            return null;
        }
        Currency source = orBase(from);
        Currency target = orBase(to);
        if (source == target) {
            return amount;
        }
        if (!source.isBase() && !target.isBase()) {
            throw new BadRequestException("Ikki xorijiy valyuta orasida to'g'ridan-to'g'ri konvertatsiya qo'llab-quvvatlanmaydi");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new BadRequestException("Konvertatsiya uchun kurs kiritilmagan");
        }
        return source.isBase()
                ? amount.divide(rate, 2, RoundingMode.HALF_UP)
                : amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Ikki valyutadan xorijiysi; ikkalasi ham so'm bo'lsa null. */
    public static Currency foreignOf(Currency a, Currency b) {
        if (!orBase(a).isBase()) {
            return a;
        }
        return !orBase(b).isBase() ? b : null;
    }

    /** "1 250 000 so'm" yoki "$1 250.50" - xato xabarlari va bildirishnomalar uchun. */
    public static String format(BigDecimal amount, Currency currency) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        String number = new DecimalFormat("#,##0.##", symbols).format(amount != null ? amount : BigDecimal.ZERO);
        return orBase(currency) == Currency.USD ? "$" + number : number + " so'm";
    }
}
