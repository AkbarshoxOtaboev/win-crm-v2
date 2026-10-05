package uz.script.wincrm.cash.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

/** Xodimning bir kunlik tushumi bitta kassa (to'lov turi) bo'yicha, kassa valyutasida. */
@Getter
@Builder
@AllArgsConstructor
public class CashHandoverSummaryResponse {
    private Long paymentTypeId;
    private String paymentTypeName;
    private Currency currency;
    /** Mijozlardan qabul qilingan to'lovlar. */
    private BigDecimal incoming;
    /** Shu xodim kiritgan yetkazib beruvchi to'lovlari. */
    private BigDecimal outgoing;
    private BigDecimal pending;
    private BigDecimal accepted;
    /** incoming - outgoing - pending - accepted. */
    private BigDecimal remaining;
}
