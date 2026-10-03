package uz.script.wincrm.currency;

import uz.script.wincrm.currency.response.ExchangeRateResponse;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Hisobot summalarini ko'rsatish valyutasiga o'giradi. Hujjatda qotirilgan kurs bo'lsa o'sha ishlatiladi,
 * aks holda hujjat sanasidagi kompaniya kursi (undan oldingi kurs bo'lmasa - joriy kurs).
 * Bitta so'rov uchun yaratiladi: kunlik kurslar shu obyekt ichida keshlanadi.
 */
public final class ReportFx {

    private final ExchangeRateService rates;
    private final Currency display;
    private final Map<Currency, Map<LocalDate, BigDecimal>> cache = new EnumMap<>(Currency.class);

    private ReportFx(ExchangeRateService rates, Currency display) {
        this.rates = rates;
        this.display = CurrencyMath.orBase(display);
    }

    public static ReportFx of(ExchangeRateService rates, Currency display) {
        return new ReportFx(rates, display);
    }

    public Currency display() {
        return display;
    }

    /**
     * @param docRate hujjatdagi kurs (1 birlik {@code from} = docRate so'm); null yoki {@code from} so'm bo'lsa e'tiborsiz
     */
    public BigDecimal convert(BigDecimal amount, Currency from, BigDecimal docRate, LocalDate date) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        Currency source = CurrencyMath.orBase(from);
        if (source == display) {
            return amount;
        }
        BigDecimal base = source.isBase()
                ? amount
                : amount.multiply(docRate != null && docRate.signum() > 0 ? docRate : rate(source, date));
        if (display.isBase()) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }
        return base.divide(rate(display, date), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal rate(Currency currency, LocalDate date) {
        if (currency.isBase()) {
            return BigDecimal.ONE;
        }
        LocalDate day = date != null ? date : LocalDate.now();
        return cache.computeIfAbsent(currency, c -> new HashMap<>())
                .computeIfAbsent(day, d -> lookup(currency, d));
    }

    private BigDecimal lookup(Currency currency, LocalDate date) {
        try {
            return rates.rateOn(currency, date);
        } catch (BadRequestException e) {
            ExchangeRateResponse current = rates.current(currency);
            if (current == null || current.getRate() == null) {
                throw e;
            }
            return current.getRate();
        }
    }
}
