package uz.script.wincrm.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.ExchangeRateSource;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(name = "Exchange rate DTO", description = "Kun uchun kursni kiritish yoki yangilash")
public class ExchangeRateDTO {

    @NotNull
    @Schema(example = "USD")
    private Currency currency;

    @NotNull
    @Schema(example = "2026-10-03")
    private LocalDate rateDate;

    @NotNull
    @DecimalMin(value = "0.0001")
    @Schema(description = "1 birlik valyuta necha so'm", example = "12850")
    private BigDecimal rate;

    @Schema(description = "null bo'lsa MANUAL", example = "MANUAL")
    private ExchangeRateSource source;
}
