package uz.script.wincrm.cash.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(name = "Cash Available Response", description = "Kassadagi joriy qoldiq (o'z valyutasida)")
public class CashAvailableResponse {

    private Long paymentTypeId;

    private String paymentTypeName;

    @Schema(description = "To'lov turi belgisi", example = "CASH")
    private String paymentTypeIcon;

    @Schema(description = "Kassa valyutasi", example = "UZS")
    private Currency currency;

    @Schema(description = "Kirim - yetkazib beruvchilarga chiqim - rahbarga topshirilgan - xarajatlar")
    private BigDecimal available;
}
