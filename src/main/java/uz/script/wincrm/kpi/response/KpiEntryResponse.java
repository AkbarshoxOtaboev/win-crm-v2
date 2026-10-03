package uz.script.wincrm.kpi.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sverka qatori: summa qaysi buyurtmadan va qanday hisoblanib yozilgani. */
@Getter
@Builder
@AllArgsConstructor
public class KpiEntryResponse {
    private Long id;
    private LocalDateTime earnedAt;
    private Long saleOrderId;
    private String clientFullName;
    /** So'mda. */
    private BigDecimal baseAmount;
    /** Buyurtma valyutasi; xorijiy bo'lsa baseAmount = sourceAmount × exchangeRate. */
    private Currency sourceCurrency;
    private BigDecimal sourceAmount;
    private BigDecimal exchangeRate;
    private BigDecimal percent;
    private BigDecimal amount;
}
