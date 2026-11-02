package uz.script.wincrm.stock.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.InsufficientStockException;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.filial.FilialContext;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.GoodsGroup;
import uz.script.wincrm.goods.UnitType;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsGroupRepository;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.goods.repository.UnitTypeRepository;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.StockTransfer;
import uz.script.wincrm.stock.mapper.StockTransferMapper;
import uz.script.wincrm.stock.repository.StockTransferRepository;
import uz.script.wincrm.stock.request.StockTransferRequest;
import uz.script.wincrm.stock.response.StockTransferResponse;
import uz.script.wincrm.stock.response.TransferTargetWarehouseResponse;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.stock.service.StockTransferService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private static final Pattern LEGACY_RETURN = Pattern.compile("^Qaytarish #(\\d+)$");

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferMapper stockTransferMapper;
    private final StockService stockService;
    private final GoodsRepository goodsRepository;
    private final GoodsGroupRepository goodsGroupRepository;
    private final UnitTypeRepository unitTypeRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public StockTransferResponse transfer(StockTransferRequest request) {

        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BadRequestException("fromWarehouseId va toWarehouseId bir xil bo'lishi mumkin emas");
        }

        Goods goods = goodsRepository.findById(request.getGoodsId())
                .orElseThrow(() -> new EntityNotFoundException("Goods not found with id: " + request.getGoodsId()));

        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with id: " + request.getFromWarehouseId()));

        Warehouse toWarehouse = FilialContext.callAs(null, () -> warehouseRepository.findById(request.getToWarehouseId()))
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with id: " + request.getToWarehouseId()));

        Filial sourceFilial = fromWarehouse.getFilial();
        Filial targetFilial = toWarehouse.getFilial();
        Long sourceFilialId = sourceFilial != null ? sourceFilial.getId() : null;
        Long targetFilialId = targetFilial != null ? targetFilial.getId() : null;
        boolean crossFilial = !Objects.equals(sourceFilialId, targetFilialId);

        if (crossFilial && (sourceFilialId == null || targetFilialId == null)) {
            throw new BadRequestException("Omborlardan biriga filial biriktirilmagan, filiallararo jo'natib bo'lmaydi");
        }
        if (goods.getFilial() != null && sourceFilialId != null && !sourceFilialId.equals(goods.getFilial().getId())) {
            throw new BadRequestException("Mahsulot va manba ombor turli filiallarga tegishli");
        }

        boolean window = goods.getType() == Type.WINDOW;
        BigDecimal pieces = request.getCount();
        BigDecimal requestedKvm = window ? sheetsToKvm(goods, pieces) : pieces;
        BigDecimal[] movedQty = { requestedKvm };

        // Stock jadvali - yagona ishonch manbai (memory: "Stock as single source of truth")
        BigDecimal unitCost = FilialContext.callAs(sourceFilialId, () -> {
            BigDecimal available = stockService.getAvailableStock(request.getGoodsId(), request.getFromWarehouseId());
            if (window) {
                BigDecimal availablePieces = StockPieces.derive(goods, available);
                if (availablePieces == null || pieces.compareTo(availablePieces) > 0) {
                    throw new InsufficientStockException(
                            "Manba ombordagi mahsulot yetarli emas. Mavjud: " + availablePieces,
                            request.getGoodsId(),
                            request.getFromWarehouseId(),
                            availablePieces != null ? availablePieces : available,
                            pieces);
                }
                if (pieces.compareTo(availablePieces) == 0) {
                    movedQty[0] = available;
                }
            }
            if (available.compareTo(movedQty[0]) < 0) {
                BigDecimal availableQty = window ? StockPieces.derive(goods, available) : available;
                throw new InsufficientStockException(
                        "Manba ombordagi mahsulot yetarli emas. Mavjud: " + availableQty,
                        request.getGoodsId(),
                        request.getFromWarehouseId(),
                        availableQty != null ? availableQty : available,
                        pieces);
            }
            BigDecimal cost = stockService.getUnitCost(request.getGoodsId(), request.getFromWarehouseId());
            stockService.decreaseStock(request.getGoodsId(), request.getFromWarehouseId(), movedQty[0], window ? pieces : null);
            return cost;
        });

        Goods targetGoods = goods;
        if (crossFilial) {
            GoodsSnapshot snapshot = FilialContext.callAs(sourceFilialId, () -> GoodsSnapshot.of(goods));
            targetGoods = FilialContext.callAs(targetFilialId, () -> resolveTargetGoods(snapshot));
        }

        Long targetGoodsId = targetGoods.getId();
        // Maqsad omborga kirim -> StockHistory IN avtomatik yoziladi (maqsad filial nomidan)
        FilialContext.runAs(crossFilial ? targetFilialId : sourceFilialId, () ->
                stockService.increaseStock(targetGoodsId, request.getToWarehouseId(), movedQty[0],
                        window ? pieces : movedQty[0], unitCost));

        StockTransfer transfer = StockTransfer.builder()
                .goods(goods)
                .toGoods(crossFilial ? targetGoods : null)
                .fromWarehouse(fromWarehouse)
                .toWarehouse(toWarehouse)
                .toFilial(targetFilial)
                .filial(sourceFilial)
                .count(pieces)
                .pieceCount(window ? pieces : null)
                .comment(request.getComment())
                .status(Status.ACTIVE)
                .build();

        StockTransfer saved = stockTransferRepository.save(transfer);

        return stockTransferMapper.toResponse(saved, FilialContext.getFilialId());
    }

    /** Oyna listi: dona soni × eni × bo‘yi / 10000 = kv.m. */
    private BigDecimal sheetsToKvm(Goods goods, BigDecimal pieces) {
        BigDecimal width = goods.getWidth();
        BigDecimal height = goods.getHeight();
        if (width == null || height == null || width.signum() <= 0 || height.signum() <= 0) {
            throw new BadRequestException("Oyna (list) uchun eni va bo‘yi kerak. Soni bo‘yicha ko‘chirib bo‘lmaydi.");
        }
        return pieces.multiply(width).multiply(height)
                .divide(BigDecimal.valueOf(10_000), 4, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public StockTransferResponse returnTransfer(Long id) {
        ReturnPlan plan = FilialContext.callAs(null, () -> {
            StockTransfer original = stockTransferRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found with id: " + id));
            if (returnOf(original) != null) {
                throw new BadRequestException("Orqaga qaytarilgan yozuvni qayta qaytarib bo'lmaydi");
            }
            if (stockTransferRepository.existsByReturnOfId(original.getId())
                    || stockTransferRepository.existsByComment("Qaytarish #" + original.getId())) {
                throw new BadRequestException("Bu transfer allaqachon qaytarilgan");
            }
            Goods sourceGoods = original.getGoods();
            Goods held = original.getToGoods() != null ? original.getToGoods() : sourceGoods;
            Warehouse from = original.getToWarehouse();
            Warehouse to = original.getFromWarehouse();
            if (held == null || from == null || to == null) {
                throw new BadRequestException("Transferni qaytarib bo'lmaydi");
            }
            BigDecimal qty = original.getPieceCount();
            if (qty == null && sourceGoods != null && sourceGoods.getType() == Type.WINDOW) {
                qty = StockPieces.derive(sourceGoods, original.getCount());
            }
            if (qty == null) {
                qty = original.getCount();
            }
            Filial holder = original.getToFilial() != null ? original.getToFilial() : from.getFilial();
            return new ReturnPlan(
                    held.getId(),
                    from.getId(),
                    to.getId(),
                    qty,
                    holder != null ? holder.getId() : null,
                    original.getId());
        });
        if (plan.qty() == null || plan.qty().signum() <= 0) {
            throw new BadRequestException("Qaytariladigan son topilmadi");
        }
        StockTransferRequest back = new StockTransferRequest();
        back.setGoodsId(plan.goodsId());
        back.setFromWarehouseId(plan.fromWarehouseId());
        back.setToWarehouseId(plan.toWarehouseId());
        back.setCount(plan.qty());
        back.setComment("Qaytarish #" + plan.transferId());
        StockTransferResponse created = plan.holderFilialId() == null
                ? transfer(back)
                : FilialContext.callAs(plan.holderFilialId(), () -> transfer(back));
        FilialContext.callAs(null, () -> {
            StockTransfer saved = stockTransferRepository.findById(created.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found with id: " + created.getId()));
            saved.setReturnOfId(plan.transferId());
            stockTransferRepository.save(saved);
            return null;
        });
        created.setReturnOfId(plan.transferId());
        created.setMovement("RETURN");
        created.setReturned(false);
        return created;
    }

    private Long returnOf(StockTransfer transfer) {
        if (transfer.getReturnOfId() != null) {
            return transfer.getReturnOfId();
        }
        String comment = transfer.getComment();
        if (comment == null) {
            return null;
        }
        Matcher matcher = LEGACY_RETURN.matcher(comment.trim());
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    private List<StockTransferResponse> present(List<StockTransfer> transfers, Long viewerFilialId) {
        Set<Long> returnedIds = new HashSet<>();
        for (StockTransfer transfer : transfers) {
            Long of = returnOf(transfer);
            if (of != null) {
                returnedIds.add(of);
            }
        }
        return transfers.stream().map(transfer -> {
            StockTransferResponse response = stockTransferMapper.toResponse(transfer, viewerFilialId);
            Long of = returnOf(transfer);
            response.setReturnOfId(of);
            response.setMovement(of != null ? "RETURN" : "TRANSFER");
            response.setReturned(returnedIds.contains(transfer.getId()));
            return response;
        }).toList();
    }

    private record ReturnPlan(
            Long goodsId,
            Long fromWarehouseId,
            Long toWarehouseId,
            BigDecimal qty,
            Long holderFilialId,
            Long transferId
    ) {
    }

    /**
     * Har bir filialning o'z katalogi bor: avval shtrix-kod, keyin nom+tur bo'yicha qidiriladi,
     * topilmasa maqsad filialda mahsulot (guruh va o'lchov birligi bilan) yaratiladi.
     */
    private Goods resolveTargetGoods(GoodsSnapshot source) {
        if (source.barcode() != null && !source.barcode().isBlank()) {
            var byBarcode = goodsRepository.findFirstByBarcodeOrderByIdAsc(source.barcode());
            if (byBarcode.isPresent()) {
                return byBarcode.get();
            }
        }
        var byName = goodsRepository.findFirstByNameIgnoreCaseAndTypeOrderByIdAsc(source.name(), source.type());
        if (byName.isPresent()) {
            return byName.get();
        }

        GoodsGroup group = goodsGroupRepository.findFirstByNameIgnoreCaseOrderByIdAsc(source.groupName())
                .orElseGet(() -> goodsGroupRepository.save(GoodsGroup.builder()
                        .name(source.groupName())
                        .status(Status.ACTIVE)
                        .build()));
        UnitType unit = unitTypeRepository.findFirstByNameIgnoreCaseOrderByIdAsc(source.unitName())
                .orElseGet(() -> unitTypeRepository.save(UnitType.builder()
                        .name(source.unitName())
                        .status(Status.ACTIVE)
                        .build()));

        return goodsRepository.save(Goods.builder()
                .name(source.name())
                .goodsGroup(group)
                .unitType(unit)
                .type(source.type())
                .priceCost(source.priceCost())
                .priceSelling(source.priceSelling())
                .barcode(source.barcode())
                .photo(source.photo())
                .width(source.width())
                .height(source.height())
                .maxDiscountPercent(source.maxDiscountPercent())
                .status(Status.ACTIVE)
                .build());
    }

    private record GoodsSnapshot(
            String name,
            Type type,
            String groupName,
            String unitName,
            BigDecimal priceCost,
            BigDecimal priceSelling,
            String barcode,
            String photo,
            BigDecimal width,
            BigDecimal height,
            BigDecimal maxDiscountPercent
    ) {
        static GoodsSnapshot of(Goods goods) {
            return new GoodsSnapshot(
                    goods.getName(),
                    goods.getType(),
                    goods.getGoodsGroup() != null ? goods.getGoodsGroup().getName() : "Umumiy",
                    goods.getUnitType() != null ? goods.getUnitType().getName() : "dona",
                    goods.getPriceCost(),
                    goods.getPriceSelling(),
                    goods.getBarcode(),
                    goods.getPhoto(),
                    goods.getWidth(),
                    goods.getHeight(),
                    goods.getMaxDiscountPercent()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransferResponse findById(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found with id: " + id));
        return stockTransferMapper.toResponse(transfer, FilialContext.getFilialId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransferResponse> fetchAll() {
        Long filialId = FilialContext.getFilialId();
        if (filialId == null || filialId <= 0) {
            return present(stockTransferRepository.findAll(Sort.by(Sort.Direction.DESC, "id")), null);
        }
        // Kiruvchi transferlar jo'natuvchi filialga tegishli, shuning uchun filtrsiz o'qiladi
        return FilialContext.callAs(null, () -> present(stockTransferRepository.findAllVisibleForFilial(filialId), filialId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransferResponse> fetchByWarehouseId(Long warehouseId) {
        return stockTransferRepository.findAllByFromWarehouseIdOrToWarehouseId(warehouseId, warehouseId)
                .stream()
                .map(stockTransferMapper::toResponse)
                .toList();
    }

    @Override
    public List<StockTransferResponse> fetchByGoodsId(Long goodsId) {
        return stockTransferRepository.findAllByGoodsId(goodsId)
                .stream()
                .map(stockTransferMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferTargetWarehouseResponse> fetchTargetWarehouses() {
        return FilialContext.callAs(null, () -> warehouseRepository.findAll()
                .stream()
                .filter(w -> w.getFilial() == null || w.getFilial().getStatus() != Status.DELETED)
                .map(w -> TransferTargetWarehouseResponse.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .filialId(w.getFilial() != null ? w.getFilial().getId() : null)
                        .filialName(w.getFilial() != null ? w.getFilial().getName() : null)
                        .build())
                .sorted(Comparator
                        .comparing((TransferTargetWarehouseResponse w) -> w.getFilialName() == null ? "" : w.getFilialName())
                        .thenComparing(w -> w.getName() == null ? "" : w.getName()))
                .toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock transfer not found with id: " + id));
        transfer.setStatus(Status.DELETED);
        stockTransferRepository.save(transfer);
    }
}
