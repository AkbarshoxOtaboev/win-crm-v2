package uz.script.wincrm.cutting;

import uz.script.wincrm.cutting.response.CuttingResultResponse.Remnant;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chiqindi foizi va qo'lda kiritilgan yaroqsiz tasma.
 * Tasma berilsa, ingichka tomoni shu o'lchamdan oshmaydigan chiqindi yomon hisoblanadi:
 * 212×15 kiritilsa, 15 sm va undan ingichka har qanday chiqindi tasmasi rad etiladi.
 */
final class WasteLimit {

    private WasteLimit() {
    }

    static boolean active(BigDecimal maxScrapPercent, BigDecimal wasteWidthCm, BigDecimal wasteHeightCm) {
        return maxScrapPercent != null || positive(wasteWidthCm) || positive(wasteHeightCm);
    }

    static boolean accepts(
            BigDecimal scrapPercent,
            List<Remnant> remnants,
            BigDecimal maxScrapPercent,
            BigDecimal wasteWidthCm,
            BigDecimal wasteHeightCm
    ) {
        if (maxScrapPercent != null && scrapPercent != null && scrapPercent.compareTo(maxScrapPercent) > 0) {
            return false;
        }
        if (!positive(wasteWidthCm) && !positive(wasteHeightCm)) {
            return true;
        }
        if (remnants == null) {
            return true;
        }
        for (Remnant remnant : remnants) {
            if (remnant.isUseful() || remnant.getWidth() == null || remnant.getHeight() == null) {
                continue;
            }
            if (forbidden(remnant.getWidth(), remnant.getHeight(), wasteWidthCm, wasteHeightCm)) {
                return false;
            }
        }
        return true;
    }

    /** Chiqindining ingichka tomoni kiritilgan o'lcham ichiga sig'sa, bu tasma yaroqsiz. */
    static boolean forbidden(BigDecimal width, BigDecimal height, BigDecimal wasteWidthCm, BigDecimal wasteHeightCm) {
        BigDecimal thin = width.min(height);
        if (positive(wasteWidthCm) && positive(wasteHeightCm)) {
            return thin.compareTo(wasteWidthCm.min(wasteHeightCm)) <= 0;
        }
        BigDecimal limit = positive(wasteWidthCm) ? wasteWidthCm : wasteHeightCm;
        return limit != null && thin.compareTo(limit) <= 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
