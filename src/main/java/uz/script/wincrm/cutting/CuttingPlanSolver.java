package uz.script.wincrm.cutting;

import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse.Remnant;
import uz.script.wincrm.cutting.response.CuttingResultResponse.SheetLayout;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bir oiladagi (masalan faqat oq, yoki faqat qoraytirilgan) turli o'lchamlarni
 * bir nechta list turidan joylaydi. Avval foydali qoldiq, keyin yangi list.
 * Yangi listdan chiqqan foydali qoldiq keyingi o'lchamga sig'sa, o'sha qoldiqdan olinadi.
 * Bir nechta list turi bo'lsa, shu kesishga ko'proq sig'adigan va keyingi o'lchamga
 * qoldiq qoldiradigani tanlanadi.
 */
public final class CuttingPlanSolver {

    private CuttingPlanSolver() {
    }

    public record Demand(long itemId, BigDecimal pieceWidth, BigDecimal pieceHeight, int quantity) {
    }

    public record SheetChoice(long goodsId, BigDecimal width, BigDecimal height) {
    }

    public record StockRemnant(long id, long goodsId, BigDecimal width, BigDecimal height, int pieceCount) {
    }

    public record SheetUse(long goodsId, int pieces) {
    }

    public record Pick(Long remnantId, long goodsId, BigDecimal width, BigDecimal height, int quantity, boolean planned) {
    }

    public record Assignment(long itemId, List<SheetUse> sheets, List<Pick> picks) {
    }

    /** "3 mm Ok 260*180" va "Ok 3 mm 260*160,5" bitta oila: qalinlik + so'zlar, o'lchamsiz. */
    public static String familyKey(String name, BigDecimal thickness) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        n = n.replaceAll("\\d+(?:[.,]\\d+)?\\s*[x×х*]\\s*\\d+(?:[.,]\\d+)?", " ");
        n = n.replaceAll("\\d+(?:[.,]\\d+)?\\s*(mm|sm|мм|см)", " ");
        n = n.replaceAll("\\b\\d+(?:[.,]\\d+)?\\b", " ");
        List<String> tokens = new ArrayList<>();
        for (String part : n.trim().split("\\s+")) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        tokens.sort(String::compareTo);
        String t = thickness == null ? "" : thickness.stripTrailingZeros().toPlainString();
        return t + "|" + String.join(" ", tokens);
    }

    public static List<Assignment> plan(
            List<Demand> demands,
            List<SheetChoice> sheets,
            List<StockRemnant> remnants,
            BigDecimal kerfMm,
            BigDecimal edgeCm,
            boolean allowRotate
    ) {
        return plan(demands, sheets, remnants, kerfMm, edgeCm, edgeCm, allowRotate, true);
    }

    /**
     * @param fromRemnant true — avval ombordagi qoldiq; false — faqat yangi list.
     *                    Shu kesishdan chiqqan va chegaradan katta qoldiq o'sha o'lchamga qayta ishlatilmaydi,
     *                    qolgan dona boshqa listdan olinadi.
     */
    public static List<Assignment> plan(
            List<Demand> demands,
            List<SheetChoice> sheets,
            List<StockRemnant> remnants,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate,
            boolean fromRemnant
    ) {
        if (sheets == null || sheets.isEmpty()) {
            throw new BadRequestException("Kesish uchun list o'lchami yo'q");
        }
        Map<Long, DemandState> states = new LinkedHashMap<>();
        if (demands != null) {
            for (Demand demand : demands) {
                if (demand.quantity() > 0 && positive(demand.pieceWidth()) && positive(demand.pieceHeight())) {
                    states.put(demand.itemId(), new DemandState(demand));
                }
            }
        }
        List<Rect> pool = new ArrayList<>();
        if (remnants != null) {
            for (StockRemnant remnant : remnants) {
                if (remnant.pieceCount() > 0 && positive(remnant.width()) && positive(remnant.height())) {
                    pool.add(new Rect(remnant.id(), remnant.goodsId(), remnant.width(), remnant.height(), remnant.pieceCount(), false, null, null));
                }
            }
        }
        List<Event> events = new ArrayList<>();
        int guard = 0;
        while (remaining(states) > 0) {
            if (++guard > 10_000) {
                throw new BadRequestException("Kesish rejasi juda katta");
            }
            if (fromRemnant && fillFromPool(states, sheets, pool, events, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate)) {
                continue;
            }
            if (!openSheet(states, sheets, pool, events, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate)) {
                DemandState stuck = states.values().stream().filter(s -> s.left > 0).findFirst().orElseThrow();
                throw new BadRequestException("Bo'lak " + fmt(stuck.pw) + "×" + fmt(stuck.ph) + " sm tanlangan listlarga sig'maydi");
            }
        }
        return toAssignments(states, events);
    }

    /** Qoldiqdan olishda kichikroq o'lcham birinchi — katta bo'lak yangi listda qoladi. */
    private static boolean fillFromPool(
            Map<Long, DemandState> states,
            List<SheetChoice> sheets,
            List<Rect> pool,
            List<Event> events,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        Choice best = null;
        for (Rect rect : pool) {
            if (rect.available < 1) {
                continue;
            }
            for (DemandState demand : states.values()) {
                if (demand.left < 1 || samePiece(rect.blockW, rect.blockH, demand.pw, demand.ph)) {
                    continue;
                }
                int per = perSheet(rect.w, rect.h, demand, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                if (per < 1) {
                    continue;
                }
                int place = Math.min(demand.left, per);
                CuttingResultResponse laid = cut(rect.w, rect.h, demand, place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                if (oversizedArea(laid).signum() > 0
                        && hasCleanSheet(sheets, demand, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate)) {
                    continue;
                }
                Choice choice = new Choice(rect, demand, place, demand.pw.multiply(demand.ph), rect.w.multiply(rect.h));
                if (best == null || betterRemnant(choice, best)) {
                    best = choice;
                }
            }
        }
        if (best == null) {
            return false;
        }
        best.rect.available--;
        best.demand.left -= best.place;
        boolean planned = best.rect.remnantId == null;
        events.add(new Event(
                best.demand.id, null, best.rect.remnantId, best.rect.goodsId,
                best.rect.w, best.rect.h, best.place, planned
        ));
        addUseful(pool, best.rect.goodsId, best.demand, cut(best.rect.w, best.rect.h, best.demand, best.place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate));
        return true;
    }

    private static boolean betterRemnant(Choice choice, Choice best) {
        int area = choice.pieceArea.compareTo(best.pieceArea);
        if (area != 0) {
            return area < 0;
        }
        if (choice.place != best.place) {
            return choice.place > best.place;
        }
        return choice.rectArea.compareTo(best.rectArea) < 0;
    }

    private static boolean openSheet(
            Map<Long, DemandState> states,
            List<SheetChoice> sheets,
            List<Rect> pool,
            List<Event> events,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        Open best = null;
        for (SheetChoice sheet : sheets) {
            if (!positive(sheet.width()) || !positive(sheet.height())) {
                continue;
            }
            for (DemandState demand : states.values()) {
                if (demand.left < 1) {
                    continue;
                }
                int per = perSheet(sheet.width(), sheet.height(), demand, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                if (per < 1) {
                    continue;
                }
                int place = Math.min(demand.left, per);
                CuttingResultResponse laid = cut(sheet.width(), sheet.height(), demand, place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                BigDecimal primary = demand.pw.multiply(demand.ph).multiply(BigDecimal.valueOf(place));
                BigDecimal bonus = bonusArea(sheet, demand, place, states, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                BigDecimal sheetArea = sheet.width().multiply(sheet.height());
                Open open = new Open(sheet, demand, place, primary, bonus, sheetArea, oversizedArea(laid));
                if (best == null || betterSheet(open, best)) {
                    best = open;
                }
            }
        }
        if (best == null) {
            return false;
        }
        best.demand.left -= best.place;
        events.add(new Event(
                best.demand.id, best.sheet.goodsId(), null, best.sheet.goodsId(),
                best.sheet.width(), best.sheet.height(), best.place, false
        ));
        addUseful(pool, best.sheet.goodsId(), best.demand, cut(best.sheet.width(), best.sheet.height(), best.demand, best.place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate));
        return true;
    }

    private static boolean betterSheet(Open open, Open best) {
        int oversized = open.oversized.compareTo(best.oversized);
        if (oversized != 0) {
            return oversized < 0;
        }
        int primary = open.primary.compareTo(best.primary);
        if (primary != 0) {
            return primary > 0;
        }
        int bonus = open.bonus.compareTo(best.bonus);
        if (bonus != 0) {
            return bonus > 0;
        }
        return open.sheetArea.compareTo(best.sheetArea) < 0;
    }

    /** Shu listdan chiqadigan foydali qoldiqqa boshqa o'lchamlar qancha sig'ishini baholaydi. */
    private static BigDecimal bonusArea(
            SheetChoice sheet,
            DemandState primary,
            int place,
            Map<Long, DemandState> states,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        CuttingResultResponse result = cut(sheet.width(), sheet.height(), primary, place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
        List<Rect> temp = new ArrayList<>();
        addUseful(temp, sheet.goodsId(), primary, result);
        Map<Long, Integer> left = new LinkedHashMap<>();
        for (DemandState state : states.values()) {
            int qty = state.left;
            if (state.id == primary.id) {
                qty -= place;
            }
            if (qty > 0) {
                left.put(state.id, qty);
            }
        }
        BigDecimal bonus = BigDecimal.ZERO;
        boolean progress = true;
        while (progress) {
            progress = false;
            Choice best = null;
            for (Rect rect : temp) {
                if (rect.available < 1) {
                    continue;
                }
                for (DemandState demand : states.values()) {
                    int qty = left.getOrDefault(demand.id, 0);
                    if (qty < 1 || samePiece(rect.blockW, rect.blockH, demand.pw, demand.ph)) {
                        continue;
                    }
                    int per = perSheet(rect.w, rect.h, demand, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
                    if (per < 1) {
                        continue;
                    }
                    Choice choice = new Choice(rect, demand, Math.min(qty, per), demand.pw.multiply(demand.ph), rect.w.multiply(rect.h));
                    if (best == null || betterRemnant(choice, best)) {
                        best = choice;
                    }
                }
            }
            if (best != null) {
                best.rect.available--;
                left.put(best.demand.id, left.get(best.demand.id) - best.place);
                bonus = bonus.add(best.pieceArea.multiply(BigDecimal.valueOf(best.place)));
                progress = true;
            }
        }
        return bonus;
    }

    /** Chegaradan katta qoldiq qoladigan kesish. Bunday bo'lsa dona qoldiqsiz sig'adigan listdan olinadi. */
    private static BigDecimal oversizedArea(CuttingResultResponse result) {
        BigDecimal sum = BigDecimal.ZERO;
        if (result.getSheets() == null) {
            return sum;
        }
        for (SheetLayout layout : result.getSheets()) {
            if (layout.getRemnants() == null) {
                continue;
            }
            int copies = layout.getCount() == null ? 1 : layout.getCount();
            for (Remnant remnant : layout.getRemnants()) {
                if (remnant.isUseful() && positive(remnant.getWidth()) && positive(remnant.getHeight())) {
                    sum = sum.add(remnant.getWidth().multiply(remnant.getHeight()).multiply(BigDecimal.valueOf(copies)));
                }
            }
        }
        return sum;
    }

    private static boolean hasCleanSheet(
            List<SheetChoice> sheets,
            DemandState demand,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        if (sheets == null) {
            return false;
        }
        for (SheetChoice sheet : sheets) {
            if (!positive(sheet.width()) || !positive(sheet.height())) {
                continue;
            }
            int per = perSheet(sheet.width(), sheet.height(), demand, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
            if (per < 1) {
                continue;
            }
            int place = Math.min(demand.left, per);
            CuttingResultResponse laid = cut(sheet.width(), sheet.height(), demand, place, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate);
            if (oversizedArea(laid).signum() == 0) {
                return true;
            }
        }
        return false;
    }

    private static int perSheet(
            BigDecimal sheetWidth,
            BigDecimal sheetHeight,
            DemandState demand,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        try {
            return cut(sheetWidth, sheetHeight, demand, 1, kerfMm, edgeWidthCm, edgeHeightCm, allowRotate).getPerSheet();
        } catch (BadRequestException ex) {
            return 0;
        }
    }

    private static CuttingResultResponse cut(
            BigDecimal sheetWidth,
            BigDecimal sheetHeight,
            DemandState demand,
            int quantity,
            BigDecimal kerfMm,
            BigDecimal edgeWidthCm,
            BigDecimal edgeHeightCm,
            boolean allowRotate
    ) {
        return CuttingCalculator.calculate(new CuttingInput(
                sheetWidth,
                sheetHeight,
                demand.pw,
                demand.ph,
                quantity,
                kerfMm,
                allowRotate,
                edgeWidthCm,
                edgeWidthCm,
                edgeHeightCm
        ));
    }

    private static void addUseful(List<Rect> pool, long goodsId, DemandState source, CuttingResultResponse result) {
        if (result.getSheets() == null) {
            return;
        }
        for (SheetLayout layout : result.getSheets()) {
            int copies = layout.getCount() == null ? 1 : layout.getCount();
            if (layout.getRemnants() == null || copies < 1) {
                continue;
            }
            for (Remnant remnant : layout.getRemnants()) {
                if (!remnant.isUseful() || !positive(remnant.getWidth()) || !positive(remnant.getHeight())) {
                    continue;
                }
                pool.add(new Rect(null, goodsId, remnant.getWidth(), remnant.getHeight(), copies, true, source.pw, source.ph));
            }
        }
    }

    private static List<Assignment> toAssignments(Map<Long, DemandState> states, List<Event> events) {
        List<Assignment> out = new ArrayList<>();
        for (Long itemId : states.keySet()) {
            Map<Long, Integer> bySheet = new LinkedHashMap<>();
            List<Pick> picks = new ArrayList<>();
            for (Event event : events) {
                if (event.itemId != itemId) {
                    continue;
                }
                if (event.sheetGoodsId != null) {
                    bySheet.merge(event.sheetGoodsId, event.quantity, Integer::sum);
                } else {
                    picks.add(new Pick(event.remnantId, event.goodsId, event.width, event.height, event.quantity, event.planned));
                }
            }
            List<SheetUse> sheets = new ArrayList<>();
            bySheet.forEach((goodsId, pieces) -> sheets.add(new SheetUse(goodsId, pieces)));
            out.add(new Assignment(itemId, sheets, picks));
        }
        return out;
    }

    private static int remaining(Map<Long, DemandState> states) {
        int sum = 0;
        for (DemandState state : states.values()) {
            sum += state.left;
        }
        return sum;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String fmt(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final class DemandState {
        final long id;
        final BigDecimal pw;
        final BigDecimal ph;
        int left;

        DemandState(Demand demand) {
            this.id = demand.itemId();
            this.pw = demand.pieceWidth();
            this.ph = demand.pieceHeight();
            this.left = demand.quantity();
        }
    }

    private static final class Rect {
        final Long remnantId;
        final long goodsId;
        final BigDecimal w;
        final BigDecimal h;
        int available;
        final boolean planned;
        /** Shu o'lcham shu qoldiqdan olinmaydi: u chegaradan katta qolgan va boshqa listdan yopiladi. */
        final BigDecimal blockW;
        final BigDecimal blockH;

        Rect(Long remnantId, long goodsId, BigDecimal w, BigDecimal h, int available, boolean planned,
             BigDecimal blockW, BigDecimal blockH) {
            this.remnantId = remnantId;
            this.goodsId = goodsId;
            this.w = w;
            this.h = h;
            this.available = available;
            this.planned = planned;
            this.blockW = blockW;
            this.blockH = blockH;
        }
    }

    private static boolean samePiece(BigDecimal aw, BigDecimal ah, BigDecimal bw, BigDecimal bh) {
        if (aw == null || ah == null || bw == null || bh == null) {
            return false;
        }
        boolean direct = aw.compareTo(bw) == 0 && ah.compareTo(bh) == 0;
        boolean turned = aw.compareTo(bh) == 0 && ah.compareTo(bw) == 0;
        return direct || turned;
    }

    private record Choice(Rect rect, DemandState demand, int place, BigDecimal pieceArea, BigDecimal rectArea) {
    }

    private record Open(SheetChoice sheet, DemandState demand, int place, BigDecimal primary, BigDecimal bonus, BigDecimal sheetArea, BigDecimal oversized) {
    }

    private record Event(
            long itemId,
            Long sheetGoodsId,
            Long remnantId,
            long goodsId,
            BigDecimal width,
            BigDecimal height,
            int quantity,
            boolean planned
    ) {
    }
}
