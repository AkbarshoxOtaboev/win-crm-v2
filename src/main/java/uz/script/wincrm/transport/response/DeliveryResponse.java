package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.transport.enums.DeliveryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(name = "Transport Delivery Response")
public class DeliveryResponse {

    private Long id;
    private DeliveryStatus deliveryStatus;

    private Long saleOrderId;
    private SalesOrderStatus saleOrderStatus;
    private LocalDateTime orderDate;
    private LocalDateTime plannedDeliveryDate;
    private BigDecimal orderTotalSum;
    private String sellerFullName;
    private String orderComment;

    private Long clientId;
    private String clientFullName;
    private String clientPhone;
    private String address;

    private Long driverId;
    private String driverFullName;
    private String driverPhone;
    private String carModel;
    private String carNumber;
    private List<WorkerRef> workers;

    private String note;
    private LocalDateTime sentAt;
    private LocalDateTime acceptedAt;
    private String acceptedByName;
    private LocalDateTime departedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime confirmedAt;
    private String confirmedByName;
    private LocalDateTime cancelledAt;

    private BigDecimal salaryPercent;
    private BigDecimal salaryTotal;

    private List<Item> items;

    @Getter
    @Setter
    @Builder
    public static class WorkerRef {
        private Long id;
        private String fullName;
        private String phone;
    }

    @Getter
    @Setter
    @Builder
    public static class Item {
        private Long id;
        private String goodsName;
        private String goodsType;
        private BigDecimal count;
        private BigDecimal width;
        private BigDecimal height;
    }
}
