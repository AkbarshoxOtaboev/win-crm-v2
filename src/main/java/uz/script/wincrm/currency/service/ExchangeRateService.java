package uz.script.wincrm.currency.service;

import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.dto.ExchangeRateDTO;
import uz.script.wincrm.currency.response.CbuRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateService {

    List<ExchangeRateResponse> history(Currency currency, LocalDate from, LocalDate to);

    /** Bugun amal qiladigan kurs (bugungi yoki oxirgi oldingi); kiritilmagan bo'lsa null. */
    ExchangeRateResponse current(Currency currency);

    ExchangeRateResponse save(ExchangeRateDTO dto);

    void delete(Long id);

    CbuRateResponse cbu(Currency currency, LocalDate date);

    /** Berilgan kunda amal qiladigan kurs; UZS uchun 1. Kurs kiritilmagan bo'lsa xato. */
    BigDecimal rateOn(Currency currency, LocalDate date);
}
