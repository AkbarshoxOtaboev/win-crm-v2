package uz.script.wincrm.sale.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyAmount;
import uz.script.wincrm.sale.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(name = "Seller Debt Response", description = "Sotuvchi buyurtmalari bo'yicha qarzdor mijozlar")
public class SellerDebtResponse {

    private Long userId;

    private String userFullName;

    @Schema(description = "Sotuvchi buyurtmalari bo'yicha so'mdagi jami qolgan qarz")
    private BigDecimal totalDebt;

    @Schema(description = "Har bir valyutadagi jami qolgan qarz")
    private List<CurrencyAmount> debts;

    /** Mijoz + valyuta bo'yicha: bitta mijozning so'm va dollar buyurtmalari alohida qatorda. */
    private List<ClientDebt> clients;

    @Getter
    @Setter
    @Builder
    @Schema(name = "Seller Client Debt")
    public static class ClientDebt {

        private Long clientId;

        private String clientFullName;

        private String phone;

        private Currency currency;

        private BigDecimal totalSum;

        private BigDecimal paidSum;

        private BigDecimal debt;

        private List<OrderDebt> orders;
    }

    @Getter
    @Setter
    @Builder
    @Schema(name = "Seller Order Debt")
    public static class OrderDebt {

        private Long saleOrderId;

        private LocalDateTime orderDate;

        private BigDecimal totalSum;

        private BigDecimal paidSum;

        private BigDecimal debtSum;

        private Currency currency;

        private SalesOrderStatus status;
    }
}
