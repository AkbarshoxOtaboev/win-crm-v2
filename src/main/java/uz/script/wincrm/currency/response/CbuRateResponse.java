package uz.script.wincrm.currency.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Markaziy bank kursi - faqat taklif, avtomatik saqlanmaydi. */
@Getter
@Builder
@AllArgsConstructor
public class CbuRateResponse {
    private Currency currency;
    /** CBU qaytargan kurs sanasi (so'ralgan kundan oldingi bo'lishi mumkin). */
    private LocalDate rateDate;
    private BigDecimal rate;
    private BigDecimal diff;
}
