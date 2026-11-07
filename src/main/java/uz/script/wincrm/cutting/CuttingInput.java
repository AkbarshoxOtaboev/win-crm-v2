package uz.script.wincrm.cutting;

import java.math.BigDecimal;

/**
 * O'lchamlar santimetrda, kesuv qalinligi millimetrda.
 */
public record CuttingInput(
        BigDecimal sheetWidth,
        BigDecimal sheetHeight,
        BigDecimal pieceWidth,
        BigDecimal pieceHeight,
        int quantity,
        BigDecimal kerfMm,
        boolean allowRotate,
        BigDecimal minRemnantCm,
        BigDecimal minRemnantWidthCm,
        BigDecimal minRemnantHeightCm,
        /** To'liq listdagi chiqindi shu foizdan oshmasin. Bo'sh bo'lsa cheklov yo'q. */
        BigDecimal maxScrapPercent,
        /** Yaroqsiz tasma eni, sm. Ingichka tomoni shu qiymatdan oshmasa, bunday joylashuv yomon. */
        BigDecimal wasteWidthCm,
        /** Yaroqsiz tasma bo'yi, sm. */
        BigDecimal wasteHeightCm
) {
    public CuttingInput(
            BigDecimal sheetWidth,
            BigDecimal sheetHeight,
            BigDecimal pieceWidth,
            BigDecimal pieceHeight,
            int quantity,
            BigDecimal kerfMm,
            boolean allowRotate,
            BigDecimal minRemnantCm,
            BigDecimal minRemnantWidthCm,
            BigDecimal minRemnantHeightCm
    ) {
        this(sheetWidth, sheetHeight, pieceWidth, pieceHeight, quantity, kerfMm, allowRotate,
                minRemnantCm, minRemnantWidthCm, minRemnantHeightCm, null, null, null);
    }
}
