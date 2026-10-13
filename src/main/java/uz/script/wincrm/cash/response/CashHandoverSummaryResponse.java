package uz.script.wincrm.cash.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

/** Bitta kassaning (to'lov turi) bir kunlik harakati, kassa valyutasida. Kassa barcha xodimlar uchun umumiy. */
@Getter
@Builder
@AllArgsConstructor
public class CashHandoverSummaryResponse {
    private Long paymentTypeId;
    private String paymentTypeName;
    private Currency currency;
    /** Kun boshidagi qoldiq (oldingi kunlardan qolgan). */
    private BigDecimal opening;
    /** Mijozlardan qabul qilingan to'lovlar. */
    private BigDecimal incoming;
    /** Kassadan qilingan xarajatlar va yetkazib beruvchi to'lovlari. */
    private BigDecimal outgoing;
    private BigDecimal pending;
    private BigDecimal accepted;
    /** opening + incoming - outgoing - pending - accepted; kassaning hozirgi qoldig'idan oshmaydi va 0 dan kam emas. */
    private BigDecimal remaining;
}
