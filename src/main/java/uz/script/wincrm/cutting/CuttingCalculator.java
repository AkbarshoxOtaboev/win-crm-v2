package uz.script.wincrm.cutting;

import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse.PlacedPiece;
import uz.script.wincrm.cutting.response.CuttingResultResponse.Remnant;
import uz.script.wincrm.cutting.response.CuttingResultResponse.SheetLayout;
import uz.script.wincrm.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Listni bir xil to'rtburchak bo'laklarga panjara (guillotine) usulida kesish hisobi.
 * Bo'laklar qator-qator joylashadi; qoldiq ikki kesish tartibidan foydaliroq bo'lgani bo'yicha ajratiladi.
 */
public final class CuttingCalculator {

    private static final BigDecimal CM2_PER_M2 = BigDecimal.valueOf(10_000);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int LENGTH_SCALE = 2;
    private static final int AREA_SCALE = 4;
    private static final int MAX_QUANTITY = 100_000;
    private static final BigDecimal MAX_KERF_MM = BigDecimal.valueOf(50);

    private CuttingCalculator() {
    }

    public static CuttingResultResponse calculate(CuttingInput in) {
        validate(in);
        BigDecimal kerf = in.kerfMm().movePointLeft(1);

        List<Plan> plans = new ArrayList<>();
        plans.add(plan(in, kerf, in.pieceWidth(), in.pieceHeight(), false));
        if (in.allowRotate() && in.pieceWidth().compareTo(in.pieceHeight()) != 0) {
            plans.add(plan(in, kerf, in.pieceHeight(), in.pieceWidth(), true));
        }
        List<Plan> usable = plans.stream().filter(p -> p.perSheet > 0).toList();
        if (usable.isEmpty()) {
            throw new BadRequestException(
                    "Bo'lak (" + fmt(in.pieceWidth()) + "×" + fmt(in.pieceHeight()) + " sm) listga ("
                            + fmt(in.sheetWidth()) + "×" + fmt(in.sheetHeight()) + " sm) sig'maydi");
        }
        // Chiqindi foizi yoki yaroqsiz tasma berilsa, avval shu cheklovga sig'adigan joylashuv.
        // Sig'masa, list soni ko'paysa ham chiqindisi kichigi olinadi.
        List<Plan> pool = usable;
        if (WasteLimit.active(in.maxScrapPercent(), in.wasteWidthCm(), in.wasteHeightCm())) {
            List<Plan> accepted = usable.stream().filter(p -> accepts(in, p)).toList();
            if (!accepted.isEmpty()) {
                pool = accepted;
            } else {
                pool = usable.stream()
                        .sorted(Comparator.comparing((Plan p) -> scrapPercent(in, p)).thenComparingInt(p -> -p.perSheet))
                        .limit(1)
                        .toList();
            }
        }
        Plan best = pool.stream()
                .min(Comparator.<Plan>comparingInt(p -> -p.perSheet)
                        .thenComparing(p -> p.fullScrap)
                        .thenComparing(p -> p.usefulArea, Comparator.reverseOrder())
                        .thenComparing(p -> p.rotated))
                .orElseThrow();
        return best.toResponse(in);
    }

    private static void validate(CuttingInput in) {
        if (!positive(in.sheetWidth()) || !positive(in.sheetHeight())) {
            throw new BadRequestException("List o'lchami (eni va bo'yi) musbat bo'lishi kerak");
        }
        if (!positive(in.pieceWidth()) || !positive(in.pieceHeight())) {
            throw new BadRequestException("Bo'lak o'lchami (eni va bo'yi) musbat bo'lishi kerak");
        }
        if (in.quantity() < 1 || in.quantity() > MAX_QUANTITY) {
            throw new BadRequestException("Dona soni 1 dan " + MAX_QUANTITY + " gacha bo'lishi kerak");
        }
        if (in.kerfMm() == null || in.kerfMm().signum() < 0 || in.kerfMm().compareTo(MAX_KERF_MM) > 0) {
            throw new BadRequestException("Kesuv qalinligi 0 dan " + MAX_KERF_MM + " mm gacha bo'lishi kerak");
        }
        if (in.minRemnantCm() == null || in.minRemnantCm().signum() < 0) {
            throw new BadRequestException("Foydali qoldiqning minimal o'lchami manfiy bo'lmasligi kerak");
        }
        if (limit(in.minRemnantWidthCm(), in.minRemnantCm()).signum() < 0
                || limit(in.minRemnantHeightCm(), in.minRemnantCm()).signum() < 0) {
            throw new BadRequestException("Foydali qoldiqning minimal o'lchami manfiy bo'lmasligi kerak");
        }
    }

    /** En va bo'y berilmasa, bitta chegara ikkala tomonga qo'llanadi. */
    private static BigDecimal limit(BigDecimal side, BigDecimal fallback) {
        return side != null ? side : fallback;
    }

    /** Qoldiq belgilangan en va bo'ydan katta (yoki teng): butun saqlanadi, ichiga kesilmaydi. */
    private static boolean keepWhole(BigDecimal width, BigDecimal height, BigDecimal minWidth, BigDecimal minHeight) {
        boolean direct = width.compareTo(minWidth) >= 0 && height.compareTo(minHeight) >= 0;
        boolean turned = width.compareTo(minHeight) >= 0 && height.compareTo(minWidth) >= 0;
        return direct || turned;
    }

    private static Plan plan(CuttingInput in, BigDecimal kerf, BigDecimal pw, BigDecimal ph, boolean rotated) {
        int cols = fit(in.sheetWidth(), pw, kerf);
        int rows = fit(in.sheetHeight(), ph, kerf);
        Plan p = new Plan(pw, ph, kerf, rotated, cols, rows);
        if (p.grid == 0) {
            return p;
        }
        // To'liq panjara qoldiqlariga (masalan 260×40) boshqa yo'nalishda sig'adigan bo'laklar ham shu listga kiradi.
        int capacity = p.layout(in, 1, p.grid + MAX_QUANTITY).pieces;
        p.perSheet = capacity;
        if (capacity > 0) {
            Group full = p.layout(in, 1, capacity);
            BigDecimal placed = area(pw, ph).multiply(BigDecimal.valueOf(full.pieces));
            p.fullScrap = area(in.sheetWidth(), in.sheetHeight()).subtract(placed).subtract(full.usefulArea).max(BigDecimal.ZERO);
            p.fullRemnants = full.remnants;
        }
        int q = in.quantity();
        int sheets = (q + capacity - 1) / capacity;
        int last = q - (sheets - 1) * capacity;
        if (last == capacity) {
            p.groups.add(p.layout(in, sheets, last));
        } else {
            if (sheets > 1) {
                p.groups.add(p.layout(in, sheets - 1, capacity));
            }
            p.groups.add(p.layout(in, 1, last));
        }
        p.sheets = sheets;
        p.usefulArea = p.groups.stream()
                .map(g -> g.usefulArea.multiply(BigDecimal.valueOf(g.count)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return p;
    }

    /** Uzunlikka necha bo'lak sig'adi: n·a + (n-1)·k ≤ L. */
    private static int fit(BigDecimal length, BigDecimal piece, BigDecimal kerf) {
        return length.add(kerf).divide(piece.add(kerf), 0, RoundingMode.FLOOR).intValue();
    }

    /** n ta bo'lak va ular orasidagi kesuvlar egallagan uzunlik. */
    private static BigDecimal span(int n, BigDecimal piece, BigDecimal kerf) {
        if (n <= 0) {
            return BigDecimal.ZERO;
        }
        return piece.multiply(BigDecimal.valueOf(n)).add(kerf.multiply(BigDecimal.valueOf(n - 1L)));
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    private static BigDecimal area(BigDecimal w, BigDecimal h) {
        return w.multiply(h).divide(CM2_PER_M2, AREA_SCALE, RoundingMode.HALF_UP);
    }

    /** To'liq listdagi chiqindi foizi va yaroqsiz tasma chekloviga sig'adimi. */
    private static boolean accepts(CuttingInput in, Plan plan) {
        return WasteLimit.accepts(scrapPercent(in, plan), plan.fullRemnants, in.maxScrapPercent(), in.wasteWidthCm(), in.wasteHeightCm());
    }

    private static BigDecimal scrapPercent(CuttingInput in, Plan plan) {
        return percent(plan.fullScrap, area(in.sheetWidth(), in.sheetHeight()));
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private static String fmt(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static final class Plan {
        final BigDecimal pw;
        final BigDecimal ph;
        final BigDecimal kerf;
        final boolean rotated;
        final int cols;
        final int rows;
        /** Asosiy panjara sig'imi. */
        final int grid;
        /** Panjara + foydali qoldiq ichiga joylashgan bo'laklar. */
        int perSheet;
        final List<Group> groups = new ArrayList<>();
        int sheets;
        BigDecimal usefulArea = BigDecimal.ZERO;
        /** To'liq listning haqiqiy chiqindisi. Bir xil sig'imda shu kichigi tanlanadi. */
        BigDecimal fullScrap = BigDecimal.ZERO;
        /** To'liq listning qoldiqlari: yaroqsiz tasma shu yerda tekshiriladi. */
        List<Remnant> fullRemnants = List.of();

        Plan(BigDecimal pw, BigDecimal ph, BigDecimal kerf, boolean rotated, int cols, int rows) {
            this.pw = pw;
            this.ph = ph;
            this.kerf = kerf;
            this.rotated = rotated;
            this.cols = cols;
            this.rows = rows;
            this.grid = cols * rows;
            this.perSheet = this.grid;
        }

        Group layout(CuttingInput in, int count, int pieces) {
            int onGrid = Math.min(pieces, grid);
            int usedRows = (onGrid + cols - 1) / cols;
            int lastRow = onGrid - (usedRows - 1) * cols;
            int usedCols = usedRows == 1 ? lastRow : cols;
            BigDecimal usedW = span(usedCols, pw, kerf);
            BigDecimal usedH = span(usedRows, ph, kerf);
            BigDecimal restW = in.sheetWidth().subtract(usedW).subtract(kerf);
            BigDecimal restH = in.sheetHeight().subtract(usedH).subtract(kerf);
            BigDecimal rightX = usedW.add(kerf);
            BigDecimal bottomY = usedH.add(kerf);

            List<Remnant> verticalFirst = new ArrayList<>();
            add(verticalFirst, in, rightX, BigDecimal.ZERO, restW, in.sheetHeight());
            add(verticalFirst, in, BigDecimal.ZERO, bottomY, usedW, restH);

            List<Remnant> horizontalFirst = new ArrayList<>();
            add(horizontalFirst, in, BigDecimal.ZERO, bottomY, in.sheetWidth(), restH);
            add(horizontalFirst, in, rightX, BigDecimal.ZERO, restW, usedH);

            List<Remnant> remnants = usefulArea(horizontalFirst).compareTo(usefulArea(verticalFirst)) > 0
                    ? horizontalFirst
                    : verticalFirst;
            if (usedRows > 1 && lastRow < cols) {
                BigDecimal gapX = span(lastRow, pw, kerf).add(kerf);
                BigDecimal gapY = span(usedRows - 1, ph, kerf).add(kerf);
                add(remnants, in, gapX, gapY, usedW.subtract(gapX), ph);
            }
            List<PlacedPiece> placed = new ArrayList<>();
            int extra = nest(in, remnants, placed, pieces - onGrid);
            return new Group(count, onGrid + extra, usedCols, usedRows, lastRow, usedW, usedH, remnants, placed);
        }

        /**
         * Foydali qoldiqqa asosiy panjara sig'magan bo'laklarni joylaydi.
         * Qoldiqqa ko'proq sig'adigan yo'nalish olinadi — 260×40 ga 40×70 o'rniga 70×40.
         */
        int nest(CuttingInput in, List<Remnant> remnants, List<PlacedPiece> placed, int budget) {
            if (budget <= 0) {
                return 0;
            }
            int total = 0;
            int guard = 0;
            while (budget > 0 && guard++ < 32) {
                int bestIndex = -1;
                int bestFits = 0;
                boolean bestSwap = false;
                for (int i = 0; i < remnants.size(); i++) {
                    Remnant remnant = remnants.get(i);
                    if (remnant.isUseful() || remnant.getFitsPieces() < 1) {
                        continue;
                    }
                    int straight = fit(remnant.getWidth(), pw, kerf) * fit(remnant.getHeight(), ph, kerf);
                    int swapped = 0;
                    if (in.allowRotate() && pw.compareTo(ph) != 0) {
                        swapped = fit(remnant.getWidth(), ph, kerf) * fit(remnant.getHeight(), pw, kerf);
                    }
                    int fits = Math.max(straight, swapped);
                    if (fits > bestFits) {
                        bestFits = fits;
                        bestIndex = i;
                        bestSwap = swapped > straight;
                    }
                }
                if (bestIndex < 0) {
                    break;
                }
                Remnant host = remnants.remove(bestIndex);
                BigDecimal pieceW = bestSwap ? ph : pw;
                BigDecimal pieceH = bestSwap ? pw : ph;
                int n = Math.min(budget, bestFits);
                cutRemnant(in, host, n, pieceW, pieceH, remnants, placed);
                budget -= n;
                total += n;
            }
            return total;
        }

        void cutRemnant(
                CuttingInput in,
                Remnant host,
                int n,
                BigDecimal pieceW,
                BigDecimal pieceH,
                List<Remnant> remnants,
                List<PlacedPiece> placed
        ) {
            int columns = fit(host.getWidth(), pieceW, kerf);
            int usedRows = (n + columns - 1) / columns;
            int lastRow = n - (usedRows - 1) * columns;
            int usedCols = usedRows == 1 ? lastRow : columns;
            BigDecimal stepX = pieceW.add(kerf);
            BigDecimal stepY = pieceH.add(kerf);
            for (int i = 0; i < n; i++) {
                int column = i % columns;
                int row = i / columns;
                placed.add(PlacedPiece.builder()
                        .x(host.getX().add(stepX.multiply(BigDecimal.valueOf(column))).setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                        .y(host.getY().add(stepY.multiply(BigDecimal.valueOf(row))).setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                        .width(pieceW)
                        .height(pieceH)
                        .build());
            }
            BigDecimal usedW = span(usedCols, pieceW, kerf);
            BigDecimal usedH = span(usedRows, pieceH, kerf);
            BigDecimal restW = host.getWidth().subtract(usedW).subtract(kerf);
            BigDecimal restH = host.getHeight().subtract(usedH).subtract(kerf);
            BigDecimal rightX = host.getX().add(usedW).add(kerf);
            BigDecimal bottomY = host.getY().add(usedH).add(kerf);

            List<Remnant> verticalFirst = new ArrayList<>();
            add(verticalFirst, in, rightX, host.getY(), restW, host.getHeight());
            add(verticalFirst, in, host.getX(), bottomY, usedW, restH);

            List<Remnant> horizontalFirst = new ArrayList<>();
            add(horizontalFirst, in, host.getX(), bottomY, host.getWidth(), restH);
            add(horizontalFirst, in, rightX, host.getY(), restW, usedH);

            List<Remnant> chosen = usefulArea(horizontalFirst).compareTo(usefulArea(verticalFirst)) > 0
                    ? horizontalFirst
                    : verticalFirst;
            if (usedRows > 1 && lastRow < columns) {
                BigDecimal gapX = host.getX().add(span(lastRow, pieceW, kerf)).add(kerf);
                BigDecimal gapY = host.getY().add(span(usedRows - 1, pieceH, kerf)).add(kerf);
                add(chosen, in, gapX, gapY, usedW.subtract(span(lastRow, pieceW, kerf)).subtract(kerf), pieceH);
            }
            remnants.addAll(chosen);
        }

        void add(List<Remnant> out, CuttingInput in, BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h) {
            w = w.setScale(LENGTH_SCALE, RoundingMode.HALF_UP);
            h = h.setScale(LENGTH_SCALE, RoundingMode.HALF_UP);
            if (w.signum() <= 0 || h.signum() <= 0) {
                return;
            }
            BigDecimal minW = limit(in.minRemnantWidthCm(), in.minRemnantCm());
            BigDecimal minH = limit(in.minRemnantHeightCm(), in.minRemnantCm());
            boolean useful = keepWhole(w, h, minW, minH);
            int fits = fit(w, in.pieceWidth(), kerf) * fit(h, in.pieceHeight(), kerf);
            if (in.allowRotate()) {
                fits = Math.max(fits, fit(w, in.pieceHeight(), kerf) * fit(h, in.pieceWidth(), kerf));
            }
            out.add(Remnant.builder()
                    .x(x.setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                    .y(y.setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                    .width(w)
                    .height(h)
                    .area(area(w, h))
                    .useful(useful)
                    .fitsPieces(fits)
                    .build());
        }

        BigDecimal usefulArea(List<Remnant> remnants) {
            return remnants.stream().filter(Remnant::isUseful).map(Remnant::getArea).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        CuttingResultResponse toResponse(CuttingInput in) {
            BigDecimal sheetArea = area(in.sheetWidth(), in.sheetHeight());
            BigDecimal sheetsArea = sheetArea.multiply(BigDecimal.valueOf(sheets));
            BigDecimal piecesArea = area(in.pieceWidth(), in.pieceHeight()).multiply(BigDecimal.valueOf(in.quantity()));
            BigDecimal leftover = sheetsArea.subtract(piecesArea).max(BigDecimal.ZERO);
            BigDecimal useful = usefulArea.min(leftover);
            BigDecimal scrap = leftover.subtract(useful);
            return CuttingResultResponse.builder()
                    .sheetWidth(in.sheetWidth())
                    .sheetHeight(in.sheetHeight())
                    .pieceWidth(in.pieceWidth())
                    .pieceHeight(in.pieceHeight())
                    .quantity(in.quantity())
                    .kerfMm(in.kerfMm())
                    .minRemnantCm(in.minRemnantCm())
                    .minRemnantWidthCm(limit(in.minRemnantWidthCm(), in.minRemnantCm()))
                    .minRemnantHeightCm(limit(in.minRemnantHeightCm(), in.minRemnantCm()))
                    .rotated(rotated)
                    .layoutPieceWidth(pw)
                    .layoutPieceHeight(ph)
                    .columns(cols)
                    .rows(rows)
                    .perSheet(perSheet)
                    .sheetsNeeded(sheets)
                    .sheetArea(sheetArea)
                    .sheetsArea(sheetsArea)
                    .piecesArea(piecesArea)
                    .leftoverArea(leftover)
                    .usefulArea(useful)
                    .scrapArea(scrap)
                    .usagePercent(percent(piecesArea, sheetsArea))
                    .leftoverPercent(percent(leftover, sheetsArea))
                    .scrapPercent(percent(scrap, sheetsArea))
                    .sheets(groups.stream().map(Group::toLayout).toList())
                    .build();
        }
    }

    private record Group(int count, int pieces, int cols, int rows, int lastRow,
                         BigDecimal usedW, BigDecimal usedH, List<Remnant> remnants,
                         List<PlacedPiece> placed, BigDecimal usefulArea) {

        Group(int count, int pieces, int cols, int rows, int lastRow,
              BigDecimal usedW, BigDecimal usedH, List<Remnant> remnants, List<PlacedPiece> placed) {
            this(count, pieces, cols, rows, lastRow, usedW, usedH, remnants, placed,
                    remnants.stream().filter(Remnant::isUseful).map(Remnant::getArea).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        SheetLayout toLayout() {
            return SheetLayout.builder()
                    .count(count)
                    .pieces(pieces)
                    .columns(cols)
                    .rows(rows)
                    .lastRowPieces(lastRow)
                    .usedWidth(usedW.setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                    .usedHeight(usedH.setScale(LENGTH_SCALE, RoundingMode.HALF_UP))
                    .remnants(remnants)
                    .placed(placed)
                    .build();
        }
    }
}
