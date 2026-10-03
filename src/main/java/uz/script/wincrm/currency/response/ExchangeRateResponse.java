package uz.script.wincrm.currency.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRateSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ExchangeRateResponse {
    private Long id;
    private Currency currency;
    private LocalDate rateDate;
    private BigDecimal rate;
    /** Oldingi kursga nisbatan o'zgarish (so'm); oldingi kurs bo'lmasa null. */
    private BigDecimal change;
    private ExchangeRateSource source;
    private String createdUsername;
    private LocalDateTime updatedAt;
}
