package uz.script.wincrm.currency.service;

import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.dto.CompanyFxRateRequest;
import uz.script.wincrm.currency.response.CompanyFxRateResponse;

import java.util.List;

/** Kompaniyaning qo'lda belgilaydigan olish/sotish kurslari (Markaziy bank kursidan alohida). */
public interface CompanyFxRateService {

    /** Har bir xorijiy valyuta uchun bitta qator; belgilanmaganlari bo'sh kurs bilan qaytadi. */
    List<CompanyFxRateResponse> fetchAll();

    CompanyFxRateResponse save(Currency currency, CompanyFxRateRequest request);
}
