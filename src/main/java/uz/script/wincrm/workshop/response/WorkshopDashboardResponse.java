package uz.script.wincrm.workshop.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.workshop.enums.WorkshopPayBasis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class WorkshopDashboardResponse {
    private Long workshopId;
    private String workshopName;
    private BigDecimal feePercent;
    private WorkshopPayBasis payBasis;
    private BigDecimal pieceRate;
    private BigDecimal balance;
    private BigDecimal totalEarned;

    private long queuedCount;
    private BigDecimal queuedSum;
    private long inProgressCount;
    private BigDecimal inProgressSum;
    private long doneCount;
    private BigDecimal doneSum;

    private List<CompletedWorkItem> completedWorks;

    @Getter
    @Setter
    @Builder
    public static class CompletedWorkItem {
        private Long assignmentId;
        private Long productionOrderId;
        private Long saleOrderId;
        private String clientFullName;
        private BigDecimal orderTotalSum;
        private BigDecimal feePercent;
        private WorkshopPayBasis payBasis;
        private BigDecimal measureQty;
        private BigDecimal pieceRate;
        private BigDecimal earnedAmount;
        private LocalDateTime acceptedAt;
        private LocalDateTime submittedAt;
        private Integer sequenceNo;
    }
}
