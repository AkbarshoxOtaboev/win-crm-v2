package uz.script.wincrm.sale.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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

    @Schema(description = "Sotuvchi buyurtmalari bo'yicha jami qolgan qarz")
    private BigDecimal totalDebt;

    private List<ClientDebt> clients;

    @Getter
    @Setter
    @Builder
    @Schema(name = "Seller Client Debt")
    public static class ClientDebt {

        private Long clientId;

        private String clientFullName;

        private String phone;

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

        private SalesOrderStatus status;
    }
}
