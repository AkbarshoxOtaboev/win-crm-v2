package uz.script.wincrm.dashboard.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Schema(name = "Cash Balance Response",
        description = "Kassa (to'lov turi) harakati o'z valyutasida: mijozlardan kirim, yetkazib beruvchilarga chiqim")
public class CashBalanceResponse {

    private Long paymentTypeId;

    private String paymentTypeName;

    @Schema(description = "Kassa valyutasi - barcha summalar shu valyutada", example = "USD")
    private Currency currency;

    @Schema(description = "Davr boshidagi qoldiq")
    private BigDecimal opening;

    @Schema(description = "Davrdagi mijoz to'lovlari")
    private BigDecimal incoming;

    @Schema(description = "Davrdagi yetkazib beruvchilarga to'lovlar")
    private BigDecimal outgoing;

    @Schema(description = "Davr oxiridagi qoldiq")
    private BigDecimal closing;
}
