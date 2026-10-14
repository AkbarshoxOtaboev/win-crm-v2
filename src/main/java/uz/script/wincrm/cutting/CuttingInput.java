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
        BigDecimal minRemnantCm
) {
}
