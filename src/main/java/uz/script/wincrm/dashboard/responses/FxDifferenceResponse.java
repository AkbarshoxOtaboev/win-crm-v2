package uz.script.wincrm.dashboard.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(name = "FX Difference Response",
        description = "Xorijiy valyutadagi buyurtma bir kursda yaratilib, to'lov boshqa kursda kelganda so'mdagi farq")
public class FxDifferenceResponse {

    @Schema(description = "Jami kurs farqi, so'mda (musbat - foyda, manfiy - zarar)")
    private BigDecimal totalDifference;

    private BigDecimal totalGain;

    private BigDecimal totalLoss;

    @Schema(description = "Davrdagi buyurtmaga taqsimlanmagan xorijiy valyutadagi to'lovlar soni (farq taqsimlanganda hisoblanadi)")
    private long unallocatedCount;

    private List<Row> rows;

    @Getter
    @Setter
    @Builder
    @Schema(name = "FX Difference Row")
    public static class Row {

        private Long paymentId;

        private LocalDateTime paymentDate;

        private Long saleOrderId;

        private LocalDateTime orderDate;

        private Long clientId;

        private String clientFullName;

        @Schema(description = "Yopilgan qarz valyutasi", example = "USD")
        private Currency debtCurrency;

        @Schema(description = "Qarzdan yopilgan summa (debtCurrency da)")
        private BigDecimal appliedAmount;

        @Schema(description = "Kassaga tushgan summa va valyuta")
        private BigDecimal paidAmount;

        private Currency paidCurrency;

        @Schema(description = "Buyurtma kursi")
        private BigDecimal orderRate;

        @Schema(description = "To'lov kunidagi kurs")
        private BigDecimal paymentRate;

        @Schema(description = "appliedAmount × orderRate")
        private BigDecimal orderBase;

        @Schema(description = "appliedAmount × paymentRate")
        private BigDecimal paymentBase;

        @Schema(description = "paymentBase - orderBase, so'mda")
        private BigDecimal difference;
    }
}
