package uz.script.wincrm.currency.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CompanyFxRateResponse {
    private Currency currency;
    /** Kompaniya valyutani shu kursda oladi; belgilanmagan bo'lsa null. */
    private BigDecimal buyRate;
    /** Kompaniya valyutani shu kursda sotadi; belgilanmagan bo'lsa null. */
    private BigDecimal sellRate;
    private String updatedUsername;
    private LocalDateTime updatedAt;
    /** Bugungi Markaziy bank kursi; olinmagan bo'lsa null. */
    private BigDecimal cbuRate;
    private BigDecimal cbuChange;
    private LocalDate cbuRateDate;
}
