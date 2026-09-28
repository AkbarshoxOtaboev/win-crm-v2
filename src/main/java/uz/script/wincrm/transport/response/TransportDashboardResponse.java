package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(name = "Transport Dashboard Response")
public class TransportDashboardResponse {
    private long pendingCount;
    private long acceptedCount;
    private long inTransitCount;
    private long arrivedCount;
    private long confirmedMonthCount;
    private BigDecimal confirmedMonthSum;
    private BigDecimal salaryPendingAmount;
    private BigDecimal salaryApprovedMonthAmount;
    private BigDecimal workerSalaryPercent;
    private long activeDrivers;
    private long activeWorkers;
    private List<DeliveryResponse> activeDeliveries;
}
