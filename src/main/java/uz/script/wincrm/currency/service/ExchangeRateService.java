package uz.script.wincrm.currency.service;

import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.response.CbuRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Kurslar faqat O'zbekiston Markaziy bankidan avtomatik olinadi; qo'lda kiritish yo'q. */
public interface ExchangeRateService {

    List<ExchangeRateResponse> history(Currency currency, LocalDate from, LocalDate to);

    /** Bugun amal qiladigan kurs (bugungi yoki oxirgi oldingi); hali olinmagan bo'lsa null. */
    ExchangeRateResponse current(Currency currency);

    /** Berilgan kunda amal qiladigan kurs - hujjatlar aynan shu kursni oladi; topilmasa null. */
    ExchangeRateResponse on(Currency currency, LocalDate date);

    /** Markaziy bank kursini o'sha bank belgilagan sana bilan saqlaydi (bor bo'lsa yangilaydi). */
    ExchangeRateResponse storeCbu(CbuRateResponse cbu);

    /** Markaziy bankdan oxirgi e'lon qilingan kursni olib saqlaydi va joriy kursni qaytaradi. */
    ExchangeRateResponse syncLatest(Currency currency);

    /** Berilgan kunda amal qiladigan kurs; UZS uchun 1. Bazada bo'lmasa Markaziy bankdan olinadi, bo'lmasa xato. */
    BigDecimal rateOn(Currency currency, LocalDate date);
}
