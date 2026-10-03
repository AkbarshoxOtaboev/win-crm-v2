package uz.script.wincrm.sale.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.sale.SaleOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * KPI, sex haqi va haydovchi maoshi doim so'mda: xorijiy valyutadagi buyurtma summasi hisoblangan kundagi
 * kurs bo'yicha o'giriladi. O'sha kunga kurs kiritilmagan bo'lsa buyurtmaning o'z kursi olinadi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SaleOrderBaseConverter {

    private final ExchangeRateService exchangeRateService;

    /** Maosh yozuviga snapshot sifatida saqlanadi: "$800 × 12 850 = 10 280 000". */
    public record Conversion(Currency currency, BigDecimal sourceAmount, BigDecimal rate, BigDecimal baseAmount) {
    }

    public BigDecimal toBaseToday(SaleOrder order, BigDecimal amount) {
        return convertToday(order, amount).baseAmount();
    }

    public Conversion convertToday(SaleOrder order, BigDecimal amount) {
        Currency currency = order != null ? order.currencyOrBase() : Currency.BASE;
        if (amount == null || currency.isBase()) {
            return new Conversion(Currency.BASE, amount, BigDecimal.ONE, amount);
        }
        BigDecimal rate;
        try {
            rate = exchangeRateService.rateOn(currency, LocalDate.now());
        } catch (Exception e) {
            log.warn("Order {}: {} kursi topilmadi, buyurtma kursi ishlatiladi: {}",
                    order.getId(), currency, e.getMessage());
            rate = order.getExchangeRate() != null ? order.getExchangeRate() : BigDecimal.ONE;
        }
        return new Conversion(currency, amount, rate, CurrencyMath.convert(amount, currency, Currency.BASE, rate));
    }
}
