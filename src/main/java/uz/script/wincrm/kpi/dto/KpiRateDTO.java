package uz.script.wincrm.kpi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class KpiRateDTO {
    /** null yoki 0 - KPI olib tashlanadi. */
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal percent;
}
