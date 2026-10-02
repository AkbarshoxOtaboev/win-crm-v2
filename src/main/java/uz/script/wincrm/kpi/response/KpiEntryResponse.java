package uz.script.wincrm.kpi.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sverka qatori: summa qaysi buyurtmadan va qanday hisoblanib yozilgani. */
@Getter
@Builder
@AllArgsConstructor
public class KpiEntryResponse {
    private Long id;
    private LocalDateTime earnedAt;
    private Long saleOrderId;
    private String clientFullName;
    private BigDecimal baseAmount;
    private BigDecimal percent;
    private BigDecimal amount;
}
