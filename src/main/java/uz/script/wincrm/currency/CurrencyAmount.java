package uz.script.wincrm.currency;

import java.math.BigDecimal;

/** Valyutasi bilan summa - turli valyutadagi summalar qo'shilmaydi, ro'yxat sifatida qaytariladi. */
public record CurrencyAmount(Currency currency, BigDecimal amount) {
}
