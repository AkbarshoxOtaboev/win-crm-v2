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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

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

        // Stock jadvali - yagona ishonch manbai (memory: "Stock as single source of truth")
        BigDecimal unitCost = FilialContext.callAs(sourceFilialId, () -> {
            BigDecimal available = stockService.getAvailableStock(request.getGoodsId(), request.getFromWarehouseId());
            if (available.compareTo(request.getCount()) < 0) {
                throw new InsufficientStockException(
                        "Manba ombordagi mahsulot yetarli emas. Mavjud: " + available + ", so'ralgan: " + request.getCount(),
                        request.getGoodsId(),
                        request.getFromWarehouseId(),
                        available,
                        request.getCount());
            }
            BigDecimal cost = stockService.getUnitCost(request.getGoodsId(), request.getFromWarehouseId());
            // Manba ombordan chiqim -> StockHistory OUT avtomatik yoziladi
            stockService.decreaseStock(request.getGoodsId(), request.getFromWarehouseId(), request.getCount());
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
                stockService.increaseStock(targetGoodsId, request.getToWarehouseId(), request.getCount(),
                        request.getCount(), unitCost));

        StockTransfer transfer = StockTransfer.builder()
                .goods(goods)
                .toGoods(crossFilial ? targetGoods : null)
                .fromWarehouse(fromWarehouse)
                .toWarehouse(toWarehouse)
                .toFilial(targetFilial)
                .filial(sourceFilial)
                .count(request.getCount())
                .comment(request.getComment())
                .status(Status.ACTIVE)
                .build();

        StockTransfer saved = stockTransferRepository.save(transfer);

        return stockTransferMapper.toResponse(saved, FilialContext.getFilialId());
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
            return stockTransferRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                    .stream()
                    .map(transfer -> stockTransferMapper.toResponse(transfer, null))
                    .toList();
        }
        // Kiruvchi transferlar jo'natuvchi filialga tegishli, shuning uchun filtrsiz o'qiladi
        return FilialContext.callAs(null, () -> stockTransferRepository.findAllVisibleForFilial(filialId)
                .stream()
                .map(transfer -> stockTransferMapper.toResponse(transfer, filialId))
                .toList());
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
