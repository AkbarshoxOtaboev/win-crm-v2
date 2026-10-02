package uz.script.wincrm.kpi.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** Xodim yoki sex bo'yicha KPI jamlanmasi. */
@Getter
@Builder
@AllArgsConstructor
public class KpiSummaryResponse {
    /** Xodim uchun userId, sex uchun workshopId. */
    private Long id;
    private String name;
    /** Xodim rollari yoki sex boshlig'i. */
    private List<String> roles;
    private String managerName;
    private BigDecimal percent;
    private long periodCount;
    private BigDecimal periodAmount;
    private BigDecimal totalAmount;
}
