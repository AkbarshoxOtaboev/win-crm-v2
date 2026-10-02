package uz.script.wincrm.kpi.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class KpiRateResponse {
    private Long userId;
    private String fullName;
    private String username;
    private List<String> roles;
    private String filialName;
    /** null - KPI belgilanmagan. */
    private BigDecimal percent;
}
