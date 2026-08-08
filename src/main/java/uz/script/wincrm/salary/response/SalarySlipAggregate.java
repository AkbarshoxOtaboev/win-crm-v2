package uz.script.wincrm.salary.response;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * SalaryTransactionRepository dagi JPQL constructor-expression uchun oraliq proyeksiya.
 * Reporting/dashboard'dagi kabi - non-transactional kontekstda lazy proxy o'rniga
 * to'g'ridan-to'g'ri DTO qaytariladi. Barcha qiymatlar musbat magnitud.
 */
@Getter
public class SalarySlipAggregate {

    private final BigDecimal totalCommission;
    private final BigDecimal totalCommissionReversal;
    private final BigDecimal totalBonus;
    private final BigDecimal totalDeduction;
    private final BigDecimal totalAdvance;

    public SalarySlipAggregate(BigDecimal totalCommission,
                               BigDecimal totalCommissionReversal,
                               BigDecimal totalBonus,
                               BigDecimal totalDeduction,
                               BigDecimal totalAdvance) {
        this.totalCommission = totalCommission;
        this.totalCommissionReversal = totalCommissionReversal;
        this.totalBonus = totalBonus;
        this.totalDeduction = totalDeduction;
        this.totalAdvance = totalAdvance;
    }
}
