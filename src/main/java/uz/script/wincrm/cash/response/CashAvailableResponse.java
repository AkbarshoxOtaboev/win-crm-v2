package uz.script.wincrm.cash.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(name = "Cash Available Response", description = "Operatsion va Rahbar kassa qoldiqlari (o'z valyutasida)")
public class CashAvailableResponse {

    private Long paymentTypeId;

    private String paymentTypeName;

    @Schema(description = "To'lov turi belgisi", example = "CASH")
    private String paymentTypeIcon;

    @Schema(description = "Kassa valyutasi", example = "UZS")
    private Currency currency;

    @Schema(description = "Operatsion (kassir) qoldiq — available bilan bir xil (eski klientlar uchun)")
    private BigDecimal available;

    @Schema(description = "Operatsion kassa: kirim - topshirish(PENDING/ACCEPTED) - operatsion chiqimlar")
    private BigDecimal operationalAvailable;

    @Schema(description = "Rahbar kassa: qabul qilingan topshirishlar - rahbar chiqimlari")
    private BigDecimal vaultAvailable;
}
