package uz.script.wincrm.transport.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(name = "Transport Worker Salary Response")
public class WorkerSalaryResponse {
    private Long id;
    private Long deliveryId;
    private Long saleOrderId;
    private String clientFullName;
    private Long workerId;
    private String workerFullName;
    private BigDecimal orderTotalSnapshot;
    @Schema(description = "Buyurtma valyutasi; xorijiy bo'lsa orderTotalSnapshot = sourceAmount × exchangeRate")
    private Currency sourceCurrency;
    private BigDecimal sourceAmount;
    private BigDecimal exchangeRate;
    private BigDecimal percentSnapshot;
    private Integer workersCount;
    private BigDecimal amount;
    private WorkerSalaryStatus salaryStatus;
    private LocalDateTime earnedAt;
    private Integer periodYear;
    private Integer periodMonth;
    private LocalDateTime decidedAt;
    private String decidedByName;
    private String comment;
}
