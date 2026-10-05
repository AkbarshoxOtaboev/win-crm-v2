package uz.script.wincrm.cash.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CashHandoverResponse {
    private Long id;
    private LocalDate handoverDate;
    private Long cashierId;
    private String cashierName;
    private Long paymentTypeId;
    private String paymentTypeName;
    private Currency currency;
    private BigDecimal expectedAmount;
    private BigDecimal amount;
    /** amount - expectedAmount: manfiy - kamomad, musbat - ortiqcha. */
    private BigDecimal difference;
    private CashHandoverStatus handoverStatus;
    private String comment;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    /** Joriy foydalanuvchining o'z topshirishi (kutilayotgan bo'lsa bekor qila oladi). */
    private boolean mine;
}
