package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Schema(name = "Transport Worker Salary Summary", description = "Per-worker totals for a month")
public class WorkerSalarySummaryResponse {
    private Long workerId;
    private String workerFullName;
    private long deliveriesCount;
    private BigDecimal pendingAmount;
    private BigDecimal approvedAmount;
}
