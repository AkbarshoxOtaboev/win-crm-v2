package uz.script.wincrm.cutting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cutting.CuttingAllocation;
import uz.script.wincrm.cutting.GlassRemnant;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.dto.CuttingOptimizeRequest;
import uz.script.wincrm.cutting.repository.CuttingAllocationRepository;
import uz.script.wincrm.cutting.repository.GlassRemnantRepository;
import uz.script.wincrm.cutting.response.CuttingOptimizeResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.SaleOrderWaste;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.repository.SaleOrderWasteRepository;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Buyurtma yaratilgach kesishni odam hal qiladi: donalar butun listdan yoki foydali qoldiqdan.
 * Hozircha sotuvchi (savdo yaratish/tahrir) tasdiqlaydi; keyin alohida rol qo‘yiladi.
 * Tasdiqlangach foydali bo‘laklar ombor qoldig‘iga, chiqindi esa waste yozuviga tushadi.
 */
@Service
@RequiredArgsConstructor
public class CuttingOptimizeService {

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderItemRepository itemRepository;
    private final SaleOrderWasteRepository wasteRepository;
    private final GlassRemnantRepository remnantRepository;
    private final CuttingAllocationRepository allocationRepository;
    private final CuttingService cuttingService;

    @Transactional(readOnly = true)
    public CuttingOptimizeResponse load(Long saleOrderId) {
        SaleOrder order = order(saleOrderId);
        List<SaleOrderItem> windows = windowItems(saleOrderId);
        List<CuttingAllocation> saved = allocationRepository.findBySaleOrder_Id(saleOrderId);
        Map<Long, List<CuttingAllocation>> byItem = new HashMap<>();
        for (CuttingAllocation row : saved) {
            byItem.computeIfAbsent(row.getSaleOrderItem().getId(), k -> new ArrayList<>()).add(row);
        }
        List<Long> goodsIds = windows.stream().map(i -> i.getGoods().getId()).distinct().toList();
        List<GlassRemnant> stock = remnantRepository.findByStatusAndPieceCountGreaterThan(Status.ACTIVE, 0).stream()
                .filter(r -> goodsIds.contains(r.getGoods().getId()))
                .filter(r -> order.getWarehouse() == null || Objects.equals(r.getWarehouse().getId(), order.getWarehouse().getId()))
                .toList();

        return CuttingOptimizeResponse.builder()
                .saleOrderId(order.getId())
                .warehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null)
                .warehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null)
                .optimized(!saved.isEmpty())
                .lines(windows.stream().map(item -> toLine(item, byItem.get(item.getId()))).toList())
                .remnants(stock.stream().map(this::toStock).toList())
                .build();
    }

    @Transactional
    public CuttingOptimizeResponse confirm(Long saleOrderId, CuttingOptimizeRequest request) {
        if (allocationRepository.existsBySaleOrder_Id(saleOrderId)) {
            throw new BadRequestException("Bu buyurtma kesish optimizatsiyasi allaqachon saqlangan");
        }
        SaleOrder order = order(saleOrderId);
        Warehouse warehouse = order.getWarehouse();
        if (warehouse == null) {
            throw new BadRequestException("Buyurtmada ombor yo‘q");
        }
        List<SaleOrderItem> windows = windowItems(saleOrderId);
        Map<Long, SaleOrderItem> byId = new HashMap<>();
        for (SaleOrderItem item : windows) {
            byId.put(item.getId(), item);
        }
        if (request.getLines() == null || request.getLines().size() != windows.size()) {
            throw new BadRequestException("Har bir oyna qatori uchun taqsimot kerak");
        }

        Map<Long, Integer> remnantLeft = new HashMap<>();
        List<Planned> planned = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (CuttingOptimizeRequest.Line line : request.getLines()) {
            SaleOrderItem item = byId.get(line.getSaleOrderItemId());
            if (item == null || !seen.add(item.getId())) {
                throw new BadRequestException("Oyna qatori topilmadi: " + line.getSaleOrderItemId());
            }
            int pieces = pieces(item);
            int sheetQty = line.getSheetQuantity() == null ? 0 : line.getSheetQuantity();
            int fromRemnants = 0;
            List<CuttingOptimizeRequest.RemnantPick> picks = line.getRemnants() == null ? List.of() : line.getRemnants();
            for (CuttingOptimizeRequest.RemnantPick pick : picks) {
                GlassRemnant remnant = remnantRepository.findById(pick.getRemnantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Qoldiq topilmadi: " + pick.getRemnantId()));
                if (remnant.getStatus() != Status.ACTIVE || remnant.getPieceCount() == null || remnant.getPieceCount() < 1) {
                    throw new BadRequestException("Qoldiq ishlatib bo‘lingan: " + remnant.getId());
                }
                if (!remnant.getGoods().getId().equals(item.getGoods().getId())) {
                    throw new BadRequestException("Qoldiq boshqa mahsulotga tegishli");
                }
                if (!warehouse.getId().equals(remnant.getWarehouse().getId())) {
                    throw new BadRequestException("Qoldiq boshqa omborda");
                }
                CuttingResultResponse cut = cut(remnant.getWidth(), remnant.getHeight(), item, pick.getQuantity());
                int sheets = cut.getSheetsNeeded() == null ? 0 : cut.getSheetsNeeded();
                int left = remnantLeft.getOrDefault(remnant.getId(), remnant.getPieceCount());
                if (sheets > left) {
                    throw new BadRequestException("Qoldiq yetarli emas (" + remnant.getWidth() + "×" + remnant.getHeight() + ")");
                }
                remnantLeft.put(remnant.getId(), left - sheets);
                fromRemnants += pick.getQuantity();
                planned.add(new Planned(item, remnant, pick.getQuantity(), sheets, cut));
            }
            if (sheetQty + fromRemnants != pieces) {
                throw new BadRequestException(item.getGoods().getName() + ": listdan va qoldiqdan olingan dona jami " + pieces + " bo‘lishi kerak");
            }
            if (sheetQty > 0) {
                Goods goods = item.getGoods();
                if (goods.getWidth() == null || goods.getHeight() == null) {
                    throw new BadRequestException("Mahsulotda list o‘lchami yo‘q: " + goods.getName());
                }
                CuttingResultResponse sheetCut = cut(goods.getWidth(), goods.getHeight(), item, sheetQty);
                planned.add(new Planned(item, null, sheetQty, sheetCut.getSheetsNeeded(), sheetCut));
            }
        }
        if (!seen.containsAll(byId.keySet())) {
            throw new BadRequestException("Har bir oyna qatori uchun taqsimot kerak");
        }

        for (Planned row : planned) {
            CuttingAllocation allocation = CuttingAllocation.builder()
                    .saleOrder(order)
                    .saleOrderItem(row.item)
                    .source(row.remnant == null ? CuttingAllocation.Source.SHEET : CuttingAllocation.Source.REMNANT)
                    .remnant(row.remnant)
                    .quantity(row.orderPieces)
                    .status(Status.ACTIVE)
                    .build();
            allocation.setFilial(order.getFilial());
            allocationRepository.save(allocation);
            if (row.remnant != null) {
                row.remnant.setPieceCount(row.remnant.getPieceCount() - row.sheetsUsed);
                if (row.remnant.getPieceCount() <= 0) {
                    row.remnant.setPieceCount(0);
                    row.remnant.setStatus(Status.DISABLED);
                }
                remnantRepository.save(row.remnant);
            }
            recordOffcuts(order, warehouse, row.item.getGoods(), row.cut);
        }
        return load(saleOrderId);
    }

    private CuttingOptimizeResponse.Line toLine(SaleOrderItem item, List<CuttingAllocation> rows) {
        int sheet = 0;
        List<CuttingOptimizeResponse.SavedPick> picks = new ArrayList<>();
        if (rows != null) {
            for (CuttingAllocation row : rows) {
                if (row.getSource() == CuttingAllocation.Source.SHEET) {
                    sheet += row.getQuantity();
                } else if (row.getRemnant() != null) {
                    picks.add(CuttingOptimizeResponse.SavedPick.builder()
                            .remnantId(row.getRemnant().getId())
                            .quantity(row.getQuantity())
                            .width(row.getRemnant().getWidth())
                            .height(row.getRemnant().getHeight())
                            .build());
                }
            }
        }
        return CuttingOptimizeResponse.Line.builder()
                .itemId(item.getId())
                .goodsId(item.getGoods().getId())
                .goodsName(item.getGoods().getName())
                .width(item.getWidth())
                .height(item.getHeight())
                .count(pieces(item))
                .kerfMm(item.getCutKerfMm())
                .allowRotate(item.getCutAllowRotate())
                .sheetQuantity(rows == null ? null : sheet)
                .picks(picks)
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
        return cuttingService.calculate(request);
    }

    private List<SaleOrderItem> windowItems(Long saleOrderId) {
        return itemRepository.findAllBySaleOrderId(saleOrderId).stream()
                .filter(i -> i.getGoods() != null && i.getGoods().getType() == Type.WINDOW)
                .filter(i -> i.getWidth() != null && i.getHeight() != null)
                .toList();
    }

    /** SaleOrderItem.count oyna uchun kv.m. Dona = kv.m × 10000 / (eni × bo‘yi). */
    private int pieces(SaleOrderItem item) {
        BigDecimal width = item.getWidth();
        BigDecimal height = item.getHeight();
        BigDecimal kvm = item.getCount();
        if (width == null || height == null || kvm == null
                || width.signum() <= 0 || height.signum() <= 0 || kvm.signum() <= 0) {
            throw new BadRequestException("Oyna o‘lchami yoki miqdori noto‘g‘ri");
        }
        BigDecimal raw = kvm.multiply(BigDecimal.valueOf(10_000))
                .divide(width.multiply(height), 4, RoundingMode.HALF_UP);
        BigDecimal rounded = raw.setScale(0, RoundingMode.HALF_UP);
        if (raw.subtract(rounded).abs().compareTo(new BigDecimal("0.05")) > 0) {
            throw new BadRequestException("Oyna donasi butun son bo‘lishi kerak");
        }
        return rounded.intValue();
    }

    private SaleOrder order(Long id) {
        return saleOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));
    }

    private record Planned(SaleOrderItem item, GlassRemnant remnant, int orderPieces, int sheetsUsed, CuttingResultResponse cut) {
    }
}
