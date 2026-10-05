package uz.script.wincrm.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CompanyFxRateRequest {

    @NotNull(message = "Olish kursi majburiy")
    @DecimalMin(value = "0.0001", message = "Olish kursi 0 dan katta bo'lishi kerak")
    private BigDecimal buyRate;

    @NotNull(message = "Sotish kursi majburiy")
    @DecimalMin(value = "0.0001", message = "Sotish kursi 0 dan katta bo'lishi kerak")
    private BigDecimal sellRate;
}
