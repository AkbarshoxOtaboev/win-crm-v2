package uz.script.wincrm.cutting;

import org.junit.jupiter.api.Test;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse.Remnant;
import uz.script.wincrm.cutting.response.CuttingResultResponse.SheetLayout;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuttingCalculatorTest {

    private static CuttingResultResponse calc(double sw, double sh, double pw, double ph, int qty, double kerfMm) {
        return calc(sw, sh, pw, ph, qty, kerfMm, true);
    }

    private static CuttingResultResponse calc(double sw, double sh, double pw, double ph, int qty, double kerfMm, boolean rotate) {
        return CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(sw), BigDecimal.valueOf(sh),
                BigDecimal.valueOf(pw), BigDecimal.valueOf(ph),
                qty, BigDecimal.valueOf(kerfMm), rotate, BigDecimal.valueOf(30), null, null));
    }

    private static void assertNum(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    private static Remnant remnant(List<Remnant> remnants, String w, String h) {
        return remnants.stream()
                .filter(r -> r.getWidth().compareTo(new BigDecimal(w)) == 0 && r.getHeight().compareTo(new BigDecimal(h)) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no remnant " + w + "x" + h));
    }

    @Test
    void twentyPiecesWithoutKerfFitOneSheet() {
        CuttingResultResponse r = calc(260, 180, 50, 30, 20, 0);

        assertFalse(r.isRotated());
        assertEquals(5, r.getColumns());
        assertEquals(6, r.getRows());
        assertEquals(30, r.getPerSheet());
        assertEquals(1, r.getSheetsNeeded());
        assertNum("4.68", r.getSheetsArea());
        assertNum("3.00", r.getPiecesArea());
        assertNum("1.68", r.getLeftoverArea());
        assertNum("35.9", r.getLeftoverPercent());

        SheetLayout sheet = r.getSheets().getFirst();
        assertEquals(4, sheet.getRows());
        assertEquals(5, sheet.getColumns());
        assertEquals(2, sheet.getRemnants().size());
        Remnant bottom = remnant(sheet.getRemnants(), "260", "60");
        assertTrue(bottom.isUseful());
        assertEquals(10, bottom.getFitsPieces());
        assertFalse(remnant(sheet.getRemnants(), "10", "120").isUseful());
        assertNum("1.56", r.getUsefulArea());
        assertNum("0.12", r.getScrapArea());
    }

    @Test
    void kerfLeavesOnlyFiveRowsOfThirty() {
        CuttingResultResponse r = calc(260, 180, 50, 30, 20, 3);

        assertEquals(5, r.getColumns());
        assertEquals(5, r.getRows());
        assertEquals(25, r.getPerSheet());
        assertEquals(1, r.getSheetsNeeded());
        SheetLayout sheet = r.getSheets().getFirst();
        assertNum("251.2", sheet.getUsedWidth());
        assertNum("120.9", sheet.getUsedHeight());
        remnant(sheet.getRemnants(), "260", "58.8");
        remnant(sheet.getRemnants(), "8.5", "120.9");
    }

    @Test
    void thirtyOnePiecesNeedTwoSheets() {
        CuttingResultResponse r = calc(260, 180, 50, 30, 31, 0);

        assertEquals(2, r.getSheetsNeeded());
        assertEquals(2, r.getSheets().size());
        SheetLayout full = r.getSheets().get(0);
        assertEquals(1, full.getCount());
        assertEquals(30, full.getPieces());
        SheetLayout last = r.getSheets().get(1);
        assertEquals(1, last.getPieces());
        assertNum("9.36", r.getSheetsArea());
        assertNum("4.65", r.getPiecesArea());
    }

    @Test
    void identicalFullSheetsAreGrouped() {
        CuttingResultResponse r = calc(260, 180, 50, 30, 60, 0);

        assertEquals(2, r.getSheetsNeeded());
        assertEquals(1, r.getSheets().size());
        assertEquals(2, r.getSheets().getFirst().getCount());
    }

    @Test
    void partialLastRowLeavesGap() {
        CuttingResultResponse r = calc(260, 180, 50, 30, 7, 0);

        SheetLayout sheet = r.getSheets().getFirst();
        assertEquals(2, sheet.getRows());
        assertEquals(2, sheet.getLastRowPieces());
        Remnant gap = remnant(sheet.getRemnants(), "150", "30");
        assertNum("100", gap.getX());
        assertNum("30", gap.getY());
        assertEquals(3, gap.getFitsPieces());
    }

    @Test
    void usefulStripIsCutInTheOtherDirection() {
        CuttingResultResponse full = CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(260), BigDecimal.valueOf(180),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70),
                15, BigDecimal.ZERO, true, BigDecimal.valueOf(50), null, null));

        assertFalse(full.isRotated());
        assertEquals(6, full.getColumns());
        assertEquals(2, full.getRows());
        assertEquals(15, full.getPerSheet());
        assertEquals(1, full.getSheetsNeeded());

        SheetLayout sheet = full.getSheets().getFirst();
        assertEquals(15, sheet.getPieces());
        assertEquals(3, sheet.getPlaced().size());
        assertNum("70", sheet.getPlaced().getFirst().getWidth());
        assertNum("40", sheet.getPlaced().getFirst().getHeight());
        assertNum("0", sheet.getPlaced().getFirst().getX());
        assertNum("140", sheet.getPlaced().getFirst().getY());
        assertTrue(sheet.getRemnants().stream().noneMatch(remnant -> remnant.getFitsPieces() >= 3));
        assertEquals(0, remnant(sheet.getRemnants(), "30", "40").getFitsPieces());

        CuttingResultResponse partial = CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(260), BigDecimal.valueOf(180),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70),
                12, BigDecimal.ZERO, true, BigDecimal.valueOf(50), null, null));
        SheetLayout untouched = partial.getSheets().getFirst();
        assertEquals(12, untouched.getPieces());
        assertTrue(untouched.getPlaced().isEmpty());
        assertEquals(3, remnant(untouched.getRemnants(), "240", "40").getFitsPieces());
    }

    @Test
    void thinScrapStripLosesToRotatedLayout() {
        CuttingResultResponse plain = CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(260), BigDecimal.valueOf(180),
                BigDecimal.valueOf(53), BigDecimal.valueOf(165),
                4, BigDecimal.ZERO, true, BigDecimal.valueOf(20), null, null));
        assertFalse(plain.isRotated());
        assertEquals(4, plain.getPerSheet());

        CuttingResultResponse turned = CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(260), BigDecimal.valueOf(180),
                BigDecimal.valueOf(53), BigDecimal.valueOf(165),
                4, BigDecimal.ZERO, true,
                BigDecimal.valueOf(20), BigDecimal.valueOf(20), BigDecimal.valueOf(20),
                BigDecimal.valueOf(8), BigDecimal.valueOf(212), BigDecimal.valueOf(15)));
        assertTrue(turned.isRotated());
        assertEquals(3, turned.getPerSheet());
        assertEquals(2, turned.getSheetsNeeded());
        SheetLayout full = turned.getSheets().getFirst();
        assertEquals(3, full.getPieces());
        assertTrue(full.getRemnants().stream()
                .filter(remnant -> !remnant.isUseful())
                .noneMatch(remnant -> remnant.getWidth().min(remnant.getHeight()).compareTo(BigDecimal.valueOf(15)) <= 0));
    }

    @Test
    void offcutLargerThanLimitGoesToAnotherSheet() {
        CuttingResultResponse r = CuttingCalculator.calculate(new CuttingInput(
                BigDecimal.valueOf(260), BigDecimal.valueOf(180),
                BigDecimal.valueOf(40), BigDecimal.valueOf(70),
                15, BigDecimal.ZERO, true, BigDecimal.valueOf(20), null, null));

        assertEquals(12, r.getPerSheet());
        assertEquals(2, r.getSheetsNeeded());
        assertTrue(r.getSheets().getFirst().getPlaced().isEmpty());
    }

    @Test
    void rotatesWhenMorePiecesFit() {
        CuttingResultResponse r = calc(100, 50, 50, 100, 1, 0);

        assertTrue(r.isRotated());
        assertEquals(1, r.getPerSheet());
        assertNum("100", r.getLayoutPieceWidth());
        assertNum("50", r.getLayoutPieceHeight());
    }

    @Test
    void pieceLargerThanSheetIsRejected() {
        assertThrows(BadRequestException.class, () -> calc(260, 180, 270, 190, 1, 0));
        assertThrows(BadRequestException.class, () -> calc(100, 50, 50, 100, 1, 0, false));
    }
}
