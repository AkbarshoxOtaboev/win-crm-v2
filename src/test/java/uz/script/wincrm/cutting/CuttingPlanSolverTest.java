package uz.script.wincrm.cutting;

import org.junit.jupiter.api.Test;
import uz.script.wincrm.cutting.CuttingPlanSolver.Assignment;
import uz.script.wincrm.cutting.CuttingPlanSolver.Demand;
import uz.script.wincrm.cutting.CuttingPlanSolver.Pick;
import uz.script.wincrm.cutting.CuttingPlanSolver.SheetChoice;
import uz.script.wincrm.cutting.CuttingPlanSolver.SheetUse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuttingPlanSolverTest {

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    private static int sheetPieces(Assignment assignment) {
        return assignment.sheets().stream().mapToInt(SheetUse::pieces).sum();
    }

    private static int picked(Assignment assignment) {
        return assignment.picks().stream().mapToInt(Pick::quantity).sum();
    }

    @Test
    void remnantOfFirstSizeFeedsSecond() {
        List<Assignment> plan = CuttingPlanSolver.plan(
                List.of(new Demand(10, bd(40), bd(40), 2), new Demand(11, bd(40), bd(20), 2)),
                List.of(new SheetChoice(1, bd(100), bd(100))),
                List.of(),
                bd(0), bd(10), true
        );
        Assignment second = plan.stream().filter(a -> a.itemId() == 11).findFirst().orElseThrow();
        assertEquals(0, sheetPieces(second));
        assertEquals(2, picked(second));
        assertTrue(second.picks().stream().allMatch(Pick::planned));
    }

    @Test
    void prefersSheetThatFitsOverLargerWaste() {
        List<Assignment> plan = CuttingPlanSolver.plan(
                List.of(new Demand(1, bd(90), bd(40), 1)),
                List.of(new SheetChoice(1, bd(300), bd(300)), new SheetChoice(2, bd(100), bd(50))),
                List.of(),
                bd(0), bd(10), true
        );
        assertEquals(2L, plan.getFirst().sheets().getFirst().goodsId());
        assertEquals(1, sheetPieces(plan.getFirst()));
    }

    @Test
    void edgeKeepBlocksNarrowRemnant() {
        List<Assignment> loose = CuttingPlanSolver.plan(
                List.of(new Demand(10, bd(40), bd(40), 2), new Demand(11, bd(40), bd(20), 2)),
                List.of(new SheetChoice(1, bd(100), bd(100))),
                List.of(),
                bd(0), bd(10), true
        );
        List<Assignment> tight = CuttingPlanSolver.plan(
                List.of(new Demand(10, bd(40), bd(40), 2), new Demand(11, bd(40), bd(20), 2)),
                List.of(new SheetChoice(1, bd(100), bd(100))),
                List.of(),
                bd(0), bd(70), true
        );
        Assignment looseSecond = loose.stream().filter(a -> a.itemId() == 11).findFirst().orElseThrow();
        Assignment tightSecond = tight.stream().filter(a -> a.itemId() == 11).findFirst().orElseThrow();
        assertEquals(0, sheetPieces(looseSecond));
        assertTrue(sheetPieces(tightSecond) > 0);
    }

    @Test
    void warehouseRemnantIsFilledBeforeAnotherSheet() {
        List<Assignment> loose = order13(bd(0));
        Assignment line = loose.stream().filter(a -> a.itemId() == 22).findFirst().orElseThrow();
        int onRemnant = line.picks().stream()
                .filter(p -> p.remnantId() != null && p.remnantId() == 4L)
                .mapToInt(Pick::quantity)
                .sum();
        assertEquals(12, onRemnant);
        assertEquals(50, sheetPieces(line) + picked(line));
    }

    @Test
    void kerfThreeStopsRemnantAtTenAndLeavesPartialSheet() {
        List<Assignment> tight = order13(bd(3));
        Assignment line = tight.stream().filter(a -> a.itemId() == 22).findFirst().orElseThrow();
        int onRemnant = line.picks().stream()
                .filter(p -> p.remnantId() != null && p.remnantId() == 4L)
                .mapToInt(Pick::quantity)
                .sum();
        assertEquals(10, onRemnant);
        assertEquals(37, line.sheets().stream().mapToInt(SheetUse::pieces).sum());
        assertEquals(50, sheetPieces(line) + picked(line));
    }

    private static List<Assignment> order13(BigDecimal kerf) {
        return CuttingPlanSolver.plan(
                List.of(new Demand(22, bd(40), bd(70), 50), new Demand(23, bd(60), bd(80), 30)),
                List.of(new SheetChoice(11, bd(260), bd(160.5)), new SheetChoice(9, bd(260), bd(180))),
                List.of(
                        new CuttingPlanSolver.StockRemnant(4, 11, bd(219.7), bd(160.5), 1),
                        new CuttingPlanSolver.StockRemnant(5, 11, bd(40), bd(90.2), 1),
                        new CuttingPlanSolver.StockRemnant(6, 11, bd(28.5), bd(160.5), 1),
                        new CuttingPlanSolver.StockRemnant(7, 11, bd(80.3), bd(19.9), 1)
                ),
                kerf, bd(20), true
        );
    }

    @Test
    void oversizedOffcutGoesToAnotherFullSheet() {
        List<Assignment> plan = CuttingPlanSolver.plan(
                List.of(new Demand(1, bd(42), bd(172), 12)),
                List.of(new SheetChoice(9, bd(260), bd(180)), new SheetChoice(11, bd(260), bd(160.5))),
                List.of(new CuttingPlanSolver.StockRemnant(8, 11, bd(218), bd(160.5), 1)),
                bd(0), bd(20), bd(20), true, true
        );
        Assignment line = plan.getFirst();
        assertEquals(12, sheetPieces(line));
        assertEquals(0, picked(line));
        assertEquals(1, line.sheets().size());
        assertEquals(9L, line.sheets().getFirst().goodsId());
        assertEquals(12, line.sheets().getFirst().pieces());
    }

    @Test
    void sameFamilyNameIgnoresSheetSize() {
        assertEquals(
                CuttingPlanSolver.familyKey("3mm oq oyna 260x180", bd(3)),
                CuttingPlanSolver.familyKey("3mm oq oyna 260x160,5", bd(3))
        );
        assertEquals(
                CuttingPlanSolver.familyKey("3 mm Ok 260*180", bd(3)),
                CuttingPlanSolver.familyKey("Ok 3 mm 260*160.5", bd(3))
        );
    }
}
