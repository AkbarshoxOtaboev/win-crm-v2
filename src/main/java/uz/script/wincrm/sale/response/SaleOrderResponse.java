package uz.script.wincrm.sale.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.sale.enums.DeliveryType;
import uz.script.wincrm.sale.enums.DiscountType;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Sale Order Response", description = "Sale Order information returned by the API")
public class SaleOrderResponse {

    @Schema(description = "Unique sale order identifier", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Client full name", example = "John Doe")
    private String clientFullName;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Central Warehouse")
    private String warehouseName;

    @Schema(description = "ID of the user (salesperson) who placed the order", example = "1")
    private Long userId;

    @Schema(description = "Full name of the user (salesperson) who placed the order", example = "Ali Valiyev")
    private String userFullName;

    @Schema(description = "Comment/Notes about the sale order")
    private String comment;

    @Schema(description = "Order date and time", example = "2026-07-04T09:30:15")
    private LocalDateTime orderDate;

    @Schema(description = "Buyurtma tayyor bo'lishi rejalashtirilgan sana", example = "2026-07-17T00:00:00")
    private LocalDateTime plannedReadyDate;

    @Schema(description = "Mijozga yetkazilishi rejalashtirilgan sana", example = "2026-07-18T00:00:00")
    private LocalDateTime plannedDeliveryDate;

    @Schema(description = "Chegirmagacha bo'lgan asl summa", example = "5000.00")
    private BigDecimal originalTotalSum;

    @Schema(description = "Qo'llangan chegirma turi", example = "PERCENTAGE")
    private DiscountType discountType;

    @Schema(description = "Kiritilgan chegirma qiymati (foiz yoki summa)", example = "10")
    private BigDecimal discountValue;

    @Schema(description = "Hisoblangan aniq chegirma summasi", example = "500.00")
    private BigDecimal discountAmount;

    @Schema(description = "Yakuniy summa: asl summa - chegirma + yetkazib berish haqi", example = "4500.00")
    private BigDecimal totalSum;

    @Schema(description = "Yetkazib berish turi (null - belgilanmagan eski buyurtma)", example = "DELIVERY")
    private DeliveryType deliveryType;

    @Schema(description = "Yetkazib berish xizmati haqi", example = "150000")
    private BigDecimal deliveryFee;

    @Schema(description = "Paid sum of the sale order", example = "3000.00")
    private BigDecimal paidSum;

    @Schema(description = "Remaining debt sum of the sale order", example = "1500.00")
    private BigDecimal debtSum;

    @Schema(description = "Buyurtma valyutasi; barcha summalar shu valyutada", example = "USD")
    private Currency currency;

    @Schema(description = "Buyurtma kursi: 1 birlik valyuta = ? so'm", example = "12850")
    private BigDecimal exchangeRate;

    @Schema(description = "totalSum ning buyurtma kursidagi so'm ekvivalenti", example = "19275000")
    private BigDecimal totalSumBase;

    @Schema(description = "Current sale order status", example = "ACTIVE")
    private Status status;

    @Schema(description = "Sale order processing status", example = "CONFIRMED")
    private SalesOrderStatus orderStatus;

    @Schema(description = "Date and time when the sale order was created", example = "2026-07-04T09:30:15")
    private LocalDateTime createdAt;

    @Schema(description = "Date and time when the sale order was last updated", example = "2026-07-04T11:45:30")
    private LocalDateTime updatedAt;

    @Schema(description = "ID of the user who created the sale order record", example = "1")
    private Long createdBy;
}