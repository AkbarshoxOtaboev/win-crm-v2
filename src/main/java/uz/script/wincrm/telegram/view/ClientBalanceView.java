package uz.script.wincrm.telegram.view;

import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientBalanceView(
        Currency currency,
        BigDecimal totalPurchase,
        BigDecimal totalPaid,
        BigDecimal totalDebt,
        LocalDateTime lastUpdated
) {
}