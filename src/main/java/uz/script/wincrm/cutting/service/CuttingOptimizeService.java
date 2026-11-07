package uz.script.wincrm.cutting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uz.script.wincrm.cutting.CuttingAllocation;
import uz.script.wincrm.cutting.CuttingPlanSolver;
import uz.script.wincrm.cutting.CuttingPlanSolver.Assignment;
import uz.script.wincrm.cutting.CuttingPlanSolver.Demand;
import uz.script.wincrm.cutting.CuttingPlanSolver.Pick;
import uz.script.wincrm.cutting.CuttingPlanSolver.SheetChoice;
import uz.script.wincrm.cutting.CuttingPlanSolver.StockRemnant;
import uz.script.wincrm.cutting.GlassRemnant;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.dto.CuttingOptimizeRequest;
import uz.script.wincrm.cutting.dto.CuttingSuggestRequest;
import uz.script.wincrm.cutting.repository.CuttingAllocationRepository;
import uz.script.wincrm.cutting.repository.GlassRemnantRepository;
import uz.script.wincrm.cutting.response.CuttingOptimizeResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.response.ManualLayoutResponse;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.SaleOrderWaste;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.repository.SaleOrderWasteRepository;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Chakana kesish: algoritm list turini va foydali qoldiqni o'zi tanlaydi.
 * Tasdiqlashdan oldin sotuvchi dona va listni qo'lda o'zgartira oladi.
 */
@Service
@RequiredArgsConstructor
public class CuttingOptimizeService {

    private static final BigDecimal DEFAULT_KERF = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_EDGE = new BigDecimal("20");
    private static final BigDecimal MAX_KERF = new BigDecimal("50");

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository itemRepository;
    private final SaleOrderWasteRepository wasteRepository;
    private final GlassRemnantRepository remnantRepository;
    private final CuttingAllocationRepository allocationRepository;
    private final CuttingService cuttingService;
    private final GoodsRepository goodsRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<CuttingOptimizeResponse.RemnantStock> listRemnants() {
        return remnantRepository.findByStatusAndPieceCountGreaterThan(Status.ACTIVE, 0).stream()
                .sorted(Comparator.comparing(GlassRemnant::getWidth).thenComparing(GlassRemnant::getHeight))
                .map(this::toListed)
                .toList();
    }

    /** Foydali qoldiqni ro‘yxatdan olib tashlaydi. Kesishda qayta tanlanmaydi. */
    @Transactional
    public void deleteRemnants(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("O'chirish uchun qoldiq tanlanmagan");
        }
        for (Long id : ids.stream().filter(Objects::nonNull).distinct().toList()) {
            GlassRemnant remnant = remnantRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Qoldiq topilmadi: " + id));
            remnant.setPieceCount(0);
            remnant.setStatus(Status.DELETED);
            remnantRepository.save(remnant);
        }
    }

    @Transactional(readOnly = true)
    public ManualLayoutResponse manualLayout(Long saleOrderId) {
        SaleOrder order = order(saleOrderId);
        return new ManualLayoutResponse(order.getCutManualLayout(), order.getCutManualSavedAt(),
                order.getCutManualLayoutPrevious() != null);
    }

    /** Qo'lda tahrirlangan kartani saqlaydi; oldingisi bitta qadam tarixda qoladi. */
    @Transactional
    public ManualLayoutResponse saveManualLayout(Long saleOrderId, String layout) {
        SaleOrder order = order(saleOrderId);
        checkManualLayout(layout, windowItems(saleOrderId));
        order.setCutManualLayoutPrevious(order.getCutManualLayout());
        order.setCutManualLayout(layout);
        order.setCutManualSavedAt(LocalDateTime.now());
        saleOrderRepository.save(order);
        return manualLayout(saleOrderId);
    }

    @Transactional
    public ManualLayoutResponse restoreManualLayout(Long saleOrderId) {
        SaleOrder order = order(saleOrderId);
        if (order.getCutManualLayoutPrevious() == null) {
            throw new BadRequestException("Oldingi kesish kartasi yo'q");
        }
        String current = order.getCutManualLayout();
        order.setCutManualLayout(order.getCutManualLayoutPrevious());
        order.setCutManualLayoutPrevious(current);
        order.setCutManualSavedAt(LocalDateTime.now());
        saleOrderRepository.save(order);
        return manualLayout(saleOrderId);
    }

    /** Detallar soni buyurtmadagidan farq qilmasligi kerak: varaqdagi + joylanmagan = buyurtma jami. */
    private void checkManualLayout(String layout, List<SaleOrderItem> windows) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(layout);
        } catch (JacksonException e) {
            throw new BadRequestException("Kesish kartasi noto'g'ri formatda");
        }
        if (root == null || !root.path("boards").isArray()) {
            throw new BadRequestException("Kesish kartasi noto'g'ri formatda");
        }
        Map<Long, Integer> counted = new HashMap<>();
        for (JsonNode board : root.path("boards")) {
            for (JsonNode part : board.path("parts")) {
                counted.merge(part.path("itemId").asLong(), 1, Integer::sum);
            }
        }
        for (JsonNode part : root.path("unplaced")) {
            counted.merge(part.path("itemId").asLong(), 1, Integer::sum);
        }
        for (SaleOrderItem item : windows) {
            int have = counted.getOrDefault(item.getId(), 0);
            if (have != pieces(item)) {
                throw new BadRequestException(item.getWidth() + "×" + item.getHeight()
                        + ": kartada " + have + " ta, buyurtmada " + pieces(item) + " ta detal");
            }
            counted.remove(item.getId());
        }
        if (!counted.isEmpty()) {
            throw new BadRequestException("Kartada buyurtmaga tegishli bo'lmagan detal bor");
        }
    }

    @Transactional(readOnly = true)
    public CuttingOptimizeResponse load(Long saleOrderId) {
        SaleOrder order = order(saleOrderId);
        List<SaleOrderItem> windows = windowItems(saleOrderId);
        List<CuttingAllocation> saved = allocationRepository.findBySaleOrder_Id(saleOrderId);
        if (saved.isEmpty()) {
            return suggestUnlocked(order, windows, DEFAULT_KERF, DEFAULT_EDGE, DEFAULT_EDGE, true, true, null, null, null);
        }
        return savedView(order, windows, saved);
    }

    @Transactional(readOnly = true)
    public CuttingOptimizeResponse suggest(Long saleOrderId, CuttingSuggestRequest request) {
        if (allocationRepository.existsBySaleOrder_Id(saleOrderId)) {
            throw new BadRequestException("Bu buyurtma kesish optimizatsiyasi allaqachon saqlangan");
        }
        SaleOrder order = order(saleOrderId);
        BigDecimal kerf = request == null || request.getKerfMm() == null ? DEFAULT_KERF : request.getKerfMm();
        BigDecimal edge = request == null || request.getMinRemnantCm() == null ? DEFAULT_EDGE : request.getMinRemnantCm();
        BigDecimal edgeWidth = request == null || request.getMinRemnantWidthCm() == null ? edge : request.getMinRemnantWidthCm();
        BigDecimal edgeHeight = request == null || request.getMinRemnantHeightCm() == null ? edge : request.getMinRemnantHeightCm();
        boolean rotate = request == null || request.getAllowRotate() == null || request.getAllowRotate();
        boolean fromRemnant = request == null || request.getSource() == null || !"SHEET".equalsIgnoreCase(request.getSource());
        BigDecimal maxScrap = request == null ? null : request.getMaxScrapPercent();
        BigDecimal wasteWidth = request == null ? null : request.getWasteWidthCm();
        BigDecimal wasteHeight = request == null ? null : request.getWasteHeightCm();
        checkKerf(kerf);
        if (edgeWidth.signum() < 0 || edgeHeight.signum() < 0) {
            throw new BadRequestException("Chegara qoldig'i manfiy bo'lishi mumkin emas");
        }
        if (maxScrap != null && (maxScrap.signum() < 0 || maxScrap.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("Chiqindi foizi 0 dan 100 gacha bo'lishi kerak");
        }
        return suggestUnlocked(order, windowItems(saleOrderId), kerf, edgeWidth, edgeHeight, rotate, fromRemnant, maxScrap, wasteWidth, wasteHeight);
    }

    @Transactional
    public CuttingOptimizeResponse confirm(Long saleOrderId, CuttingOptimizeRequest request) {
        if (allocationRepository.existsBySaleOrder_Id(saleOrderId)) {
            throw new BadRequestException("Bu buyurtma kesish optimizatsiyasi allaqachon saqlangan");
        }
        SaleOrder order = order(saleOrderId);
        Warehouse warehouse = order.getWarehouse();
        if (warehouse == null) {
            throw new BadRequestException("Buyurtmada ombor yo'q");
        }
        List<SaleOrderItem> windows = windowItems(saleOrderId);
        applyRules(windows, request);
        Map<Long, SaleOrderItem> byId = new HashMap<>();
        for (SaleOrderItem item : windows) {
            byId.put(item.getId(), item);
        }
        if (request.getLines() == null || request.getLines().size() != windows.size()) {
            throw new BadRequestException("Har bir oyna qatori uchun taqsimot kerak");
        }

        List<Goods> catalog = windowCatalog();
        Map<Long, Integer> remnantLeft = new HashMap<>();
        List<Job> jobs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (CuttingOptimizeRequest.Line line : request.getLines()) {
            SaleOrderItem item = byId.get(line.getSaleOrderItemId());
            if (item == null || !seen.add(item.getId())) {
                throw new BadRequestException("Oyna qatori topilmadi: " + line.getSaleOrderItemId());
            }
            int pieces = pieces(item);
            Set<Long> allowed = new HashSet<>();
            for (Goods goods : candidatesFor(item, catalog)) {
                allowed.add(goods.getId());
            }
            List<SheetCut> sheets = new ArrayList<>();
            int fromSheets = 0;
            List<CuttingOptimizeRequest.SheetPick> sheetPicks = line.getSheets() == null ? List.of() : line.getSheets();
            boolean explicitSheets = sheetPicks.stream().anyMatch(p -> p.getQuantity() != null && p.getQuantity() > 0);
            if (explicitSheets) {
                for (CuttingOptimizeRequest.SheetPick pick : sheetPicks) {
                    int qty = pick.getQuantity() == null ? 0 : pick.getQuantity();
                    if (qty < 1) {
                        continue;
                    }
                    Goods sheet = goodsRepository.findById(pick.getGoodsId())
                            .orElseThrow(() -> new ResourceNotFoundException("Mahsulot topilmadi: " + pick.getGoodsId()));
                    if (!allowed.contains(sheet.getId())) {
                        throw new BadRequestException("Bu list shu oyna uchun tanlanmagan: " + sheet.getName());
                    }
                    if (sheet.getWidth() == null || sheet.getHeight() == null) {
                        throw new BadRequestException("Mahsulotda list o'lchami yo'q: " + sheet.getName());
                    }
                    sheets.add(new SheetCut(item, sheet, qty, cut(sheet.getWidth(), sheet.getHeight(), item, qty)));
                    fromSheets += qty;
                }
            } else {
                int sheetQty = line.getSheetQuantity() == null ? 0 : line.getSheetQuantity();
                if (sheetQty > 0) {
                    Goods goods = item.getGoods();
                    if (goods.getWidth() == null || goods.getHeight() == null) {
                        throw new BadRequestException("Mahsulotda list o'lchami yo'q: " + goods.getName());
                    }
                    sheets.add(new SheetCut(item, goods, sheetQty, cut(goods.getWidth(), goods.getHeight(), item, sheetQty)));
                    fromSheets += sheetQty;
                }
            }

            int fromRemnants = 0;
            List<RemnantCut> remnants = new ArrayList<>();
            List<CuttingOptimizeRequest.RemnantPick> picks = line.getRemnants() == null ? List.of() : line.getRemnants();
            for (CuttingOptimizeRequest.RemnantPick pick : picks) {
                GlassRemnant remnant = remnantRepository.findById(pick.getRemnantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Qoldiq topilmadi: " + pick.getRemnantId()));
                if (remnant.getStatus() != Status.ACTIVE || remnant.getPieceCount() == null || remnant.getPieceCount() < 1) {
                    throw new BadRequestException("Qoldiq ishlatib bo'lingan: " + remnant.getId());
                }
                if (!allowed.contains(remnant.getGoods().getId())) {
                    throw new BadRequestException("Qoldiq boshqa mahsulotga tegishli");
                }
                if (!warehouse.getId().equals(remnant.getWarehouse().getId())) {
                    throw new BadRequestException("Qoldiq boshqa omborda");
                }
                CuttingResultResponse cut = cut(remnant.getWidth(), remnant.getHeight(), item, pick.getQuantity());
                int usedSheets = cut.getSheetsNeeded() == null ? 0 : cut.getSheetsNeeded();
                int left = remnantLeft.getOrDefault(remnant.getId(), remnant.getPieceCount());
                if (usedSheets > left) {
                    throw new BadRequestException("Qoldiq yetarli emas (" + remnant.getWidth() + "×" + remnant.getHeight() + ")");
                }
                remnantLeft.put(remnant.getId(), left - usedSheets);
                fromRemnants += pick.getQuantity();
                remnants.add(new RemnantCut(item, remnant, pick.getQuantity(), usedSheets, cut));
            }

            int fromPlanned = 0;
            List<PlannedCut> planned = new ArrayList<>();
            List<CuttingOptimizeRequest.PlannedPick> plannedPicks = line.getPlanned() == null ? List.of() : line.getPlanned();
            for (CuttingOptimizeRequest.PlannedPick pick : plannedPicks) {
                Goods goods = pick.getGoodsId() == null ? item.getGoods()
                        : goodsRepository.findById(pick.getGoodsId())
                        .orElseThrow(() -> new ResourceNotFoundException("Mahsulot topilmadi: " + pick.getGoodsId()));
                if (!allowed.contains(goods.getId())) {
                    throw new BadRequestException("Rejadagi qoldiq boshqa mahsulotga tegishli");
                }
                fromPlanned += pick.getQuantity();
                planned.add(new PlannedCut(item, goods, pick.getWidth(), pick.getHeight(), pick.getQuantity()));
            }
            if (fromSheets + fromRemnants + fromPlanned != pieces) {
                throw new BadRequestException(item.getGoods().getName() + ": listdan va qoldiqdan olingan dona jami " + pieces + " bo'lishi kerak");
            }
            jobs.add(new Job(item, sheets, remnants, planned));
        }
        if (!seen.containsAll(byId.keySet())) {
            throw new BadRequestException("Har bir oyna qatori uchun taqsimot kerak");
        }

        for (Job job : jobs) {
            moveSheetStock(warehouse, job);
        }
        for (Job job : jobs) {
            for (SheetCut row : job.sheets) {
                saveAllocation(order, row.item, CuttingAllocation.Source.SHEET, null, row.sheet, row.pieces);
                recordOffcuts(order, warehouse, row.sheet, row.cut);
            }
            for (RemnantCut row : job.remnants) {
                saveAllocation(order, row.item, CuttingAllocation.Source.REMNANT, row.remnant, row.remnant.getGoods(), row.pieces);
                row.remnant.setPieceCount(row.remnant.getPieceCount() - row.sheetsUsed);
                if (row.remnant.getPieceCount() <= 0) {
                    row.remnant.setPieceCount(0);
                    row.remnant.setStatus(Status.DISABLED);
                }
                remnantRepository.save(row.remnant);
                recordOffcuts(order, warehouse, row.remnant.getGoods(), row.cut);
            }
        }
        for (Job job : jobs) {
            for (PlannedCut row : job.planned) {
                CuttingResultResponse cut = cut(row.width, row.height, row.item, row.quantity);
                int usedSheets = cut.getSheetsNeeded() == null ? 0 : cut.getSheetsNeeded();
                GlassRemnant remnant = findFreshRemnant(order, warehouse, row.goods, row.width, row.height);
                if (remnant == null || remnant.getPieceCount() == null || remnant.getPieceCount() < usedSheets) {
                    throw new BadRequestException("Rejadagi qoldiq topilmadi: " + row.width + "×" + row.height);
                }
                remnant.setPieceCount(remnant.getPieceCount() - usedSheets);
                if (remnant.getPieceCount() <= 0) {
                    remnant.setPieceCount(0);
                    remnant.setStatus(Status.DISABLED);
                }
                remnantRepository.save(remnant);
                saveAllocation(order, row.item, CuttingAllocation.Source.REMNANT, remnant, row.goods, row.quantity);
                recordOffcuts(order, warehouse, row.goods, cut);
            }
        }
        return load(saleOrderId);
    }

    private CuttingOptimizeResponse suggestUnlocked(
            SaleOrder order,
            List<SaleOrderItem> windows,
            BigDecimal kerf,
            BigDecimal edgeWidth,
            BigDecimal edgeHeight,
            boolean rotate,
            boolean fromRemnant,
            BigDecimal maxScrapPercent,
            BigDecimal wasteWidthCm,
            BigDecimal wasteHeightCm
    ) {
        List<Goods> catalog = windowCatalog();
        List<GlassRemnant> stock = stockRemnants(order, windows, catalog);
        Map<String, List<SaleOrderItem>> groups = new LinkedHashMap<>();
        Map<Long, List<Goods>> candidates = new HashMap<>();
        for (SaleOrderItem item : windows) {
            List<Goods> list = candidatesFor(item, catalog);
            candidates.put(item.getId(), list);
            String key = list.stream().map(g -> String.valueOf(g.getId())).sorted().reduce((a, b) -> a + "," + b).orElse("");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        Map<Long, Assignment> planned = new HashMap<>();
        for (List<SaleOrderItem> group : groups.values()) {
            if (group.isEmpty()) {
                continue;
            }
            List<Goods> sheets = candidates.get(group.getFirst().getId());
            Set<Long> ids = new HashSet<>();
            for (Goods goods : sheets) {
                ids.add(goods.getId());
            }
            List<Demand> demands = new ArrayList<>();
            for (SaleOrderItem item : group) {
                demands.add(new Demand(item.getId(), item.getWidth(), item.getHeight(), pieces(item)));
            }
            List<SheetChoice> choices = sheets.stream()
                    .map(g -> new SheetChoice(g.getId(), g.getWidth(), g.getHeight()))
                    .toList();
            List<StockRemnant> rems = stock.stream()
                    .filter(r -> ids.contains(r.getGoods().getId()))
                    .map(r -> new StockRemnant(r.getId(), r.getGoods().getId(), r.getWidth(), r.getHeight(), r.getPieceCount()))
                    .toList();
            try {
                for (Assignment assignment : CuttingPlanSolver.plan(
                        demands, choices, rems, kerf, edgeWidth, edgeHeight, rotate, fromRemnant,
                        maxScrapPercent, wasteWidthCm, wasteHeightCm)) {
                    planned.put(assignment.itemId(), assignment);
                }
            } catch (BadRequestException ignored) {
                // Bu oila sig'masa, qator o'z listidan to'liq ko'rsatiladi va qo'lda tuzatiladi.
            }
        }
        List<CuttingOptimizeResponse.Line> lines = new ArrayList<>();
        for (SaleOrderItem item : windows) {
            lines.add(toSuggestedLine(item, planned.get(item.getId()), candidates.get(item.getId()), kerf, edgeWidth, rotate));
        }
        return CuttingOptimizeResponse.builder()
                .saleOrderId(order.getId())
                .warehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null)
                .warehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null)
                .optimized(false)
                .kerfMm(kerf)
                .minRemnantCm(edgeWidth)
                .minRemnantWidthCm(edgeWidth)
                .minRemnantHeightCm(edgeHeight)
                .source(fromRemnant ? "REMNANT" : "SHEET")
                .lines(lines)
                .remnants(stock.stream().map(this::toStock).toList())
                .build();
    }

    private CuttingOptimizeResponse savedView(SaleOrder order, List<SaleOrderItem> windows, List<CuttingAllocation> saved) {
        Map<Long, List<CuttingAllocation>> byItem = new HashMap<>();
        for (CuttingAllocation row : saved) {
            byItem.computeIfAbsent(row.getSaleOrderItem().getId(), k -> new ArrayList<>()).add(row);
        }
        List<Goods> catalog = windowCatalog();
        List<GlassRemnant> stock = stockRemnants(order, windows, catalog);
        BigDecimal kerf = windows.isEmpty() ? null : windows.getFirst().getCutKerfMm();
        BigDecimal edge = windows.isEmpty() ? null : windows.getFirst().getCutMinRemnantCm();
        return CuttingOptimizeResponse.builder()
                .saleOrderId(order.getId())
                .warehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null)
                .warehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null)
                .optimized(true)
                .kerfMm(kerf)
                .minRemnantCm(edge)
                .lines(windows.stream().map(item -> toSavedLine(item, byItem.get(item.getId()), candidatesFor(item, catalog))).toList())
                .remnants(stock.stream().map(this::toStock).toList())
                .build();
    }

    private void applyRules(List<SaleOrderItem> windows, CuttingOptimizeRequest request) {
        if (request.getKerfMm() != null) {
            checkKerf(request.getKerfMm());
            for (SaleOrderItem item : windows) {
                item.setCutKerfMm(request.getKerfMm());
            }
        }
        if (request.getMinRemnantCm() != null) {
            if (request.getMinRemnantCm().signum() < 0) {
                throw new BadRequestException("Chegara qoldig'i manfiy bo'lishi mumkin emas");
            }
            for (SaleOrderItem item : windows) {
                item.setCutMinRemnantCm(request.getMinRemnantCm());
            }
        }
        if (request.getAllowRotate() != null) {
            for (SaleOrderItem item : windows) {
                item.setCutAllowRotate(request.getAllowRotate());
            }
        }
        itemRepository.saveAll(windows);
    }

    private void checkKerf(BigDecimal kerf) {
        if (kerf.signum() < 0 || kerf.compareTo(MAX_KERF) > 0) {
            throw new BadRequestException("Kesuv 0 dan 50 mm gacha bo'lishi kerak");
        }
    }

    /** Boshqa list turidan kesilgan dona (va shu listdan chiqqan qoldiq) shu listning kv.m idan tushadi. */
    private void moveSheetStock(Warehouse warehouse, Job job) {
        Goods from = job.item.getGoods();
        Map<Long, int[]> piecesByGoods = new LinkedHashMap<>();
        Map<Long, Goods> goodsById = new HashMap<>();
        for (SheetCut row : job.sheets) {
            piecesByGoods.computeIfAbsent(row.sheet.getId(), k -> new int[1])[0] += row.pieces;
            goodsById.put(row.sheet.getId(), row.sheet);
        }
        for (PlannedCut row : job.planned) {
            piecesByGoods.computeIfAbsent(row.goods.getId(), k -> new int[1])[0] += row.quantity;
            goodsById.put(row.goods.getId(), row.goods);
        }
        for (Map.Entry<Long, int[]> entry : piecesByGoods.entrySet()) {
            if (entry.getKey().equals(from.getId())) {
                continue;
            }
            Goods target = goodsById.get(entry.getKey());
            BigDecimal kvm = job.item.getWidth().multiply(job.item.getHeight())
                    .multiply(BigDecimal.valueOf(entry.getValue()[0]))
                    .divide(BigDecimal.valueOf(10_000), StockPieces.SCALE, RoundingMode.HALF_UP);
            BigDecimal available = stockService.getAvailableStock(target.getId(), warehouse.getId());
            if (available == null || available.compareTo(kvm) < 0) {
                throw new BadRequestException(target.getName() + " omborida yetarli kv.m yo'q");
            }
            stockService.increaseStock(from.getId(), warehouse.getId(), kvm, StockPieces.derive(from, kvm));
            stockService.decreaseStock(target.getId(), warehouse.getId(), kvm, StockPieces.derive(target, kvm));
        }
    }

    private void saveAllocation(
            SaleOrder order,
            SaleOrderItem item,
            CuttingAllocation.Source source,
            GlassRemnant remnant,
            Goods sheetGoods,
            int quantity
    ) {
        CuttingAllocation allocation = CuttingAllocation.builder()
                .saleOrder(order)
                .saleOrderItem(item)
                .source(source)
                .remnant(remnant)
                .sheetGoods(sheetGoods)
                .quantity(quantity)
                .status(Status.ACTIVE)
                .build();
        allocation.setFilial(order.getFilial());
        allocationRepository.save(allocation);
    }

    private GlassRemnant findFreshRemnant(SaleOrder order, Warehouse warehouse, Goods goods, BigDecimal width, BigDecimal height) {
        BigDecimal w = width.setScale(2, RoundingMode.HALF_UP);
        BigDecimal h = height.setScale(2, RoundingMode.HALF_UP);
        List<GlassRemnant> matches = remnantRepository.findByStatusAndPieceCountGreaterThan(Status.ACTIVE, 0).stream()
                .filter(r -> r.getGoods().getId().equals(goods.getId()))
                .filter(r -> r.getWarehouse().getId().equals(warehouse.getId()))
                .filter(r -> sameSize(r.getWidth(), r.getHeight(), w, h))
                .toList();
        return matches.stream()
                .filter(r -> r.getSourceSaleOrder() != null && order.getId().equals(r.getSourceSaleOrder().getId()))
                .max(Comparator.comparing(GlassRemnant::getPieceCount))
                .orElseGet(() -> matches.stream().max(Comparator.comparing(GlassRemnant::getPieceCount)).orElse(null));
    }

    private boolean sameSize(BigDecimal aw, BigDecimal ah, BigDecimal bw, BigDecimal bh) {
        if (aw == null || ah == null) {
            return false;
        }
        boolean direct = aw.setScale(2, RoundingMode.HALF_UP).compareTo(bw) == 0
                && ah.setScale(2, RoundingMode.HALF_UP).compareTo(bh) == 0;
        boolean swapped = aw.setScale(2, RoundingMode.HALF_UP).compareTo(bh) == 0
                && ah.setScale(2, RoundingMode.HALF_UP).compareTo(bw) == 0;
        return direct || swapped;
    }

    private List<Goods> windowCatalog() {
        return goodsRepository.findAll().stream()
                .filter(g -> g.getType() == Type.WINDOW)
                .filter(g -> g.getStatus() == null || g.getStatus() == Status.ACTIVE)
                .filter(g -> g.getWidth() != null && g.getHeight() != null && g.getWidth().signum() > 0 && g.getHeight().signum() > 0)
                .toList();
    }

    private List<Goods> candidatesFor(SaleOrderItem item, List<Goods> catalog) {
        List<Goods> picked = new ArrayList<>();
        if (item.getCutCategory() != null && item.getCutCategory().getId() != null) {
            Long categoryId = item.getCutCategory().getId();
            for (Goods goods : catalog) {
                if (goods.getCategory() != null && categoryId.equals(goods.getCategory().getId())) {
                    picked.add(goods);
                }
            }
        }
        String raw = item.getCutCandidateGoodsIds();
        if (picked.isEmpty() && raw != null && !raw.isBlank()) {
            for (String part : raw.split(",")) {
                if (part.isBlank()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(part.trim());
                    catalog.stream().filter(g -> g.getId().equals(id)).findFirst().ifPresent(picked::add);
                } catch (NumberFormatException ignored) {
                    // buzilgan id tashlab yuboriladi
                }
            }
        }
        if (picked.isEmpty() && item.getGoods() != null) {
            String family = CuttingPlanSolver.familyKey(item.getGoods().getName(), item.getGoods().getThickness());
            for (Goods goods : catalog) {
                if (family.equals(CuttingPlanSolver.familyKey(goods.getName(), goods.getThickness()))) {
                    picked.add(goods);
                }
            }
        }
        if (item.getGoods() != null && picked.stream().noneMatch(g -> g.getId().equals(item.getGoods().getId()))) {
            if (item.getGoods().getWidth() != null && item.getGoods().getHeight() != null) {
                picked.add(0, item.getGoods());
            }
        }
        picked.sort(java.util.Comparator.comparing(Goods::getId));
        return picked;
    }

    private List<GlassRemnant> stockRemnants(SaleOrder order, List<SaleOrderItem> windows, List<Goods> catalog) {
        Set<Long> goodsIds = new HashSet<>();
        for (SaleOrderItem item : windows) {
            for (Goods goods : candidatesFor(item, catalog)) {
                goodsIds.add(goods.getId());
            }
        }
        return remnantRepository.findByStatusAndPieceCountGreaterThan(Status.ACTIVE, 0).stream()
                .filter(r -> goodsIds.contains(r.getGoods().getId()))
                .filter(r -> order.getWarehouse() == null || Objects.equals(r.getWarehouse().getId(), order.getWarehouse().getId()))
                .toList();
    }

    private CuttingOptimizeResponse.Line toSuggestedLine(
            SaleOrderItem item,
            Assignment assignment,
            List<Goods> candidates,
            BigDecimal kerf,
            BigDecimal edge,
            boolean rotate
    ) {
        List<CuttingOptimizeResponse.SheetUse> uses = new ArrayList<>();
        int sheetQty = 0;
        if (assignment != null && !assignment.sheets().isEmpty()) {
            for (CuttingPlanSolver.SheetUse use : assignment.sheets()) {
                Goods goods = findGoods(candidates, use.goodsId());
                uses.add(sheetUse(goods, use.goodsId(), use.pieces()));
                sheetQty += use.pieces();
            }
        } else if (assignment == null) {
            sheetQty = pieces(item);
            uses.add(sheetUse(item.getGoods(), item.getGoods().getId(), sheetQty));
        }
        List<CuttingOptimizeResponse.SavedPick> picks = new ArrayList<>();
        if (assignment != null) {
            for (Pick pick : assignment.picks()) {
                picks.add(CuttingOptimizeResponse.SavedPick.builder()
                        .remnantId(pick.remnantId())
                        .goodsId(pick.goodsId())
                        .quantity(pick.quantity())
                        .width(pick.width())
                        .height(pick.height())
                        .planned(pick.planned())
                        .build());
            }
        }
        return baseLine(item, candidates, kerf, edge, rotate)
                .sheetQuantity(sheetQty)
                .sheetUses(uses)
                .picks(picks)
                .build();
    }

    private CuttingOptimizeResponse.Line toSavedLine(SaleOrderItem item, List<CuttingAllocation> rows, List<Goods> candidates) {
        Map<Long, Integer> bySheet = new LinkedHashMap<>();
        List<CuttingOptimizeResponse.SavedPick> picks = new ArrayList<>();
        if (rows != null) {
            for (CuttingAllocation row : rows) {
                if (row.getSource() == CuttingAllocation.Source.SHEET) {
                    Goods sheet = row.getSheetGoods() != null ? row.getSheetGoods() : item.getGoods();
                    bySheet.merge(sheet.getId(), row.getQuantity(), Integer::sum);
                } else if (row.getRemnant() != null) {
                    picks.add(CuttingOptimizeResponse.SavedPick.builder()
                            .remnantId(row.getRemnant().getId())
                            .goodsId(row.getRemnant().getGoods().getId())
                            .quantity(row.getQuantity())
                            .width(row.getRemnant().getWidth())
                            .height(row.getRemnant().getHeight())
                            .planned(false)
                            .build());
                }
            }
        }
        List<CuttingOptimizeResponse.SheetUse> uses = new ArrayList<>();
        int sheetQty = 0;
        for (Map.Entry<Long, Integer> entry : bySheet.entrySet()) {
            Goods goods = findGoods(candidates, entry.getKey());
            if (goods == null && item.getGoods() != null && item.getGoods().getId().equals(entry.getKey())) {
                goods = item.getGoods();
            }
            uses.add(sheetUse(goods, entry.getKey(), entry.getValue()));
            sheetQty += entry.getValue();
        }
        return baseLine(item, candidates, item.getCutKerfMm(), item.getCutMinRemnantCm(), item.getCutAllowRotate() == null || item.getCutAllowRotate())
                .sheetQuantity(rows == null ? null : sheetQty)
                .sheetUses(uses)
                .picks(picks)
                .build();
    }

    private CuttingOptimizeResponse.Line.LineBuilder baseLine(
            SaleOrderItem item,
            List<Goods> candidates,
            BigDecimal kerf,
            BigDecimal edge,
            boolean rotate
    ) {
        List<CuttingOptimizeResponse.SheetOption> options = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (Goods goods : candidates) {
            ids.add(goods.getId());
            options.add(CuttingOptimizeResponse.SheetOption.builder()
                    .goodsId(goods.getId())
                    .goodsName(goods.getName())
                    .width(goods.getWidth())
                    .height(goods.getHeight())
                    .build());
        }
        return CuttingOptimizeResponse.Line.builder()
                .itemId(item.getId())
                .goodsId(item.getGoods().getId())
                .goodsName(item.getGoods().getName())
                .width(item.getWidth())
                .height(item.getHeight())
                .count(pieces(item))
                .kerfMm(kerf)
                .minRemnantCm(edge)
                .allowRotate(rotate)
                .candidateGoodsIds(ids)
                .candidates(options);
    }

    private CuttingOptimizeResponse.SheetUse sheetUse(Goods goods, Long goodsId, int pieces) {
        return CuttingOptimizeResponse.SheetUse.builder()
                .goodsId(goodsId)
                .goodsName(goods != null ? goods.getName() : null)
                .width(goods != null ? goods.getWidth() : null)
                .height(goods != null ? goods.getHeight() : null)
                .quantity(pieces)
                .build();
    }

    private Goods findGoods(List<Goods> candidates, long goodsId) {
        if (candidates == null) {
            return null;
        }
        return candidates.stream().filter(g -> g.getId().equals(goodsId)).findFirst().orElse(null);
    }

    private CuttingOptimizeResponse.RemnantStock toListed(GlassRemnant remnant) {
        return CuttingOptimizeResponse.RemnantStock.builder()
                .id(remnant.getId())
                .goodsId(remnant.getGoods().getId())
                .goodsName(remnant.getGoods().getName())
                .width(remnant.getWidth())
                .height(remnant.getHeight())
                .pieceCount(remnant.getPieceCount())
                .warehouseName(remnant.getWarehouse() != null ? remnant.getWarehouse().getName() : null)
                .sourceSaleOrderId(remnant.getSourceSaleOrder() != null ? remnant.getSourceSaleOrder().getId() : null)
                .build();
    }

    private CuttingOptimizeResponse.RemnantStock toStock(GlassRemnant remnant) {
        return CuttingOptimizeResponse.RemnantStock.builder()
                .id(remnant.getId())
                .goodsId(remnant.getGoods().getId())
                .goodsName(remnant.getGoods().getName())
                .width(remnant.getWidth())
                .height(remnant.getHeight())
                .pieceCount(remnant.getPieceCount())
                .build();
    }

    private void recordOffcuts(SaleOrder order, Warehouse warehouse, Goods goods, CuttingResultResponse result) {
        if (result.getSheets() == null) {
            return;
        }
        for (CuttingResultResponse.SheetLayout layout : result.getSheets()) {
            int sheets = layout.getCount() == null ? 1 : layout.getCount();
            if (layout.getRemnants() == null) {
                continue;
            }
            for (CuttingResultResponse.Remnant remnant : layout.getRemnants()) {
                if (remnant.isUseful()) {
                    addRemnant(goods, warehouse, order, remnant.getWidth(), remnant.getHeight(), sheets);
                }
                saveWaste(order, goods, remnant, sheets);
            }
        }
    }

    private void addRemnant(Goods goods, Warehouse warehouse, SaleOrder order, BigDecimal width, BigDecimal height, int pieces) {
        if (pieces < 1 || width == null || height == null) {
            return;
        }
        BigDecimal w = width.setScale(2, RoundingMode.HALF_UP);
        BigDecimal h = height.setScale(2, RoundingMode.HALF_UP);
        GlassRemnant existing = remnantRepository.findByStatusAndPieceCountGreaterThan(Status.ACTIVE, 0).stream()
                .filter(r -> r.getGoods().getId().equals(goods.getId()))
                .filter(r -> r.getWarehouse().getId().equals(warehouse.getId()))
                .filter(r -> r.getWidth().compareTo(w) == 0 && r.getHeight().compareTo(h) == 0)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setPieceCount(existing.getPieceCount() + pieces);
            if (existing.getSourceSaleOrder() == null) {
                existing.setSourceSaleOrder(order);
            }
            remnantRepository.save(existing);
            return;
        }
        GlassRemnant created = GlassRemnant.builder()
                .goods(goods)
                .warehouse(warehouse)
                .width(w)
                .height(h)
                .pieceCount(pieces)
                .sourceSaleOrder(order)
                .status(Status.ACTIVE)
                .build();
        created.setFilial(order.getFilial());
        remnantRepository.save(created);
    }

    private void saveWaste(SaleOrder order, Goods goods, CuttingResultResponse.Remnant remnant, int sheets) {
        BigDecimal area = remnant.getArea() == null ? BigDecimal.ZERO : remnant.getArea();
        SaleOrderWaste waste = SaleOrderWaste.builder()
                .saleOrder(order)
                .goods(goods)
                .goodsName(goods.getName())
                .unitName(goods.getUnitType() != null ? goods.getUnitType().getName() : "m2")
                .quantity(area.multiply(BigDecimal.valueOf(sheets)))
                .width(remnant.getWidth())
                .height(remnant.getHeight())
                .useful(remnant.isUseful())
                .comment(remnant.isUseful() ? "Foydali qoldiq" : "Chiqindi")
                .status(Status.ACTIVE)
                .build();
        waste.setFilial(order.getFilial());
        wasteRepository.save(waste);
    }

    private CuttingResultResponse cut(BigDecimal sheetW, BigDecimal sheetH, SaleOrderItem item, int quantity) {
        CuttingCalculateRequest request = new CuttingCalculateRequest();
        request.setSheetWidth(sheetW);
        request.setSheetHeight(sheetH);
        request.setPieceWidth(item.getWidth());
        request.setPieceHeight(item.getHeight());
        request.setQuantity(quantity);
        request.setKerfMm(item.getCutKerfMm());
        request.setAllowRotate(item.getCutAllowRotate());
        request.setMinRemnantCm(item.getCutMinRemnantCm());
        return cuttingService.calculate(request);
    }

    private List<SaleOrderItem> windowItems(Long saleOrderId) {
        return itemRepository.findAllBySaleOrderId(saleOrderId).stream()
                .filter(i -> i.getGoods() != null && i.getGoods().getType() == Type.WINDOW)
                .filter(i -> i.getWidth() != null && i.getHeight() != null)
                .toList();
    }

    /** SaleOrderItem.count oyna uchun kv.m. Dona = kv.m × 10000 / (eni × bo'yi). */
    private int pieces(SaleOrderItem item) {
        BigDecimal width = item.getWidth();
        BigDecimal height = item.getHeight();
        BigDecimal kvm = item.getCount();
        if (width == null || height == null || kvm == null
                || width.signum() <= 0 || height.signum() <= 0 || kvm.signum() <= 0) {
            throw new BadRequestException("Oyna o'lchami yoki miqdori noto'g'ri");
        }
        BigDecimal raw = kvm.multiply(BigDecimal.valueOf(10_000))
                .divide(width.multiply(height), 4, RoundingMode.HALF_UP);
        BigDecimal rounded = raw.setScale(0, RoundingMode.HALF_UP);
        if (raw.subtract(rounded).abs().compareTo(new BigDecimal("0.05")) > 0) {
            throw new BadRequestException("Oyna donasi butun son bo'lishi kerak");
        }
        return rounded.intValue();
    }

    private SaleOrder order(Long id) {
        return saleOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));
    }

    private record Job(SaleOrderItem item, List<SheetCut> sheets, List<RemnantCut> remnants, List<PlannedCut> planned) {
    }

    private record SheetCut(SaleOrderItem item, Goods sheet, int pieces, CuttingResultResponse cut) {
    }

    private record RemnantCut(SaleOrderItem item, GlassRemnant remnant, int pieces, int sheetsUsed, CuttingResultResponse cut) {
    }

    private record PlannedCut(SaleOrderItem item, Goods goods, BigDecimal width, BigDecimal height, int quantity) {
    }
}
