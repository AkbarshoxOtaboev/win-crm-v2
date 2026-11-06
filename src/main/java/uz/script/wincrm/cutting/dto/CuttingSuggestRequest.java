package uz.script.wincrm.cutting.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CuttingSuggestRequest {

    /** Kesuv, mm. Bo'sh bo'lsa 0. */
    @DecimalMin("0")
    private BigDecimal kerfMm;

    /** Chegaradan qolishi kerak bo'lgan minimal tomon, sm. En va bo'y berilmasa shu qiymat. */
    @DecimalMin("0")
    private BigDecimal minRemnantCm;

    @DecimalMin("0")
    private BigDecimal minRemnantWidthCm;

    @DecimalMin("0")
    private BigDecimal minRemnantHeightCm;

    /** REMNANT — avval qoldiq, SHEET — faqat yangi list. */
    private String source;

    private Boolean allowRotate;
}
