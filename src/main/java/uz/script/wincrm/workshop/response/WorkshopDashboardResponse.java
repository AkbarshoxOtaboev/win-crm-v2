package uz.script.wincrm.workshop.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
        private BigDecimal earnedAmount;
        private LocalDateTime acceptedAt;
        private LocalDateTime submittedAt;
        private Integer sequenceNo;
    }
}
