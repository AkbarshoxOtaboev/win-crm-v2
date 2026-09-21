package uz.script.wincrm.production.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;
import uz.script.wincrm.production.enums.ProductionOrderStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductionOrderResponse {
    private Long id;
    private Long saleOrderId;
    private String clientFullName;
    private ProductionOrderStatus productionStatus;
    private Long currentWorkshopId;
    private String currentWorkshopName;
    private ProductionAssignmentStatus currentAssignmentStatus;
    private Long currentAssignmentId;
    private LocalDateTime startedAt;
    private LocalDateTime doneAt;
    private String note;
    private LocalDateTime createdAt;
}
