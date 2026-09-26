package uz.script.wincrm.sale.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.clients.repository.ClientRepository;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.InsufficientStockException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.dto.SaleOrderItemDTO;
import uz.script.wincrm.sale.mapper.SaleOrderItemMapper;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.response.SaleOrderItemResponse;
import uz.script.wincrm.sale.service.SaleOrderItemService;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleOrderItemServiceImpl implements SaleOrderItemService {

    private final SaleOrderItemRepository repository;
    private final SaleOrderItemMapper mapper;
    private final SaleOrderRepository saleOrderRepository;
    private final ClientRepository clientRepository;
    private final GoodsRepository goodsRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockService stockService;

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "SaleOrderItem"
    )
//    @CacheEvict(value = "saleOrderItems", allEntries = true)
    @Transactional
    public SaleOrderItemResponse create(SaleOrderItemDTO dto) {
        log.info("Create sale order item. Goods ID: {}, Warehouse ID: {}, Requested count: {}",
                dto.getGoodsId(), dto.getWarehouseId(), dto.getCount());

        Goods goods = goodsRepository.findById(dto.getGoodsId())
                .orElseThrow(() -> new ResourceNotFoundException("Goods not found with id: " + dto.getGoodsId()));

        // ⭐ WINDOW (Oyna) turi uchun count = width * height (kv.m) qilib hisoblanadi.
        //    Hisoblangan qiymat dto.count ga yoziladi, shundan keyin stock validatsiya,
        //    mapper va stock chiqimi shu qiymat bilan avtomatik to'g'ri ishlaydi.
        dto.setCount(resolveCount(goods, dto.getWidth(), dto.getHeight(), dto.getCount()));

        // ⭐ SERVICE turidagi Goods uchun ombor (Stock) hisob-kitobi yuritilmaydi
        boolean isService = goods.getType() == Type.SERVICE;

        // ⭐ WAREHOUSE STOCK VALIDATION - MUHIM! (faqat PRODUCT/WINDOW uchun)
        if (!isService) {
            validateAndCheckStock(dto.getWarehouseId(), dto.getGoodsId(), dto.getCount());
        }

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.getClientId()));

        SaleOrder saleOrder = saleOrderRepository.findById(dto.getSaleOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + dto.getSaleOrderId()));
        validateSaleOrderStatus(saleOrder);

        SaleOrderItem entity = mapper.toEntity(dto);
        entity.setGoods(goods);
        entity.setWarehouse(warehouse);
        entity.setClient(client);
        entity.setSaleOrder(saleOrder);
        entity.setUser(saleOrder.getUser());
        entity.setStatus(Status.ACTIVE);

        entity = repository.save(entity);

        // Ombordan mahsulot sotildi -> Stockdan chiqim qilamiz (Stock + StockHistory OUT avtomatik)
        // SERVICE turidagi Goods uchun bu qadam o'tkazib yuboriladi
        if (!isService) {
            stockService.decreaseStock(dto.getGoodsId(), dto.getWarehouseId(), dto.getCount(),
                    sheetPieces(goods, dto.getCount()));
        }

        log.info("Sale order item created successfully. ID: {}", entity.getId());

        return mapper.toResponse(entity);
    }

    @Override
//    @Cacheable(value = "saleOrderItem", key = "#id")
    public SaleOrderItemResponse findById(Long id) {
        log.info("Fetch sale order item by id {}", id);

        SaleOrderItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order item not found with id: " + id));

        return mapper.toResponse(entity);
    }

    @Override
//    @Cacheable(value = "saleOrderItems")
    public Page<SaleOrderItemResponse> fetchAll(Pageable pageable) {
        log.info("Fetch all sale order items");

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public List<SaleOrderItemResponse> fetchBySaleOrderId(Long saleOrderId) {
        log.info("Fetch sale order items by sale order id {}", saleOrderId);

        return repository.findAllBySaleOrderId(saleOrderId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<SaleOrderItemResponse> fetchBySaleOrderIdPaginated(Long saleOrderId, Pageable pageable) {
        log.info("Fetch sale order items paginated by sale order id {}", saleOrderId);

        return repository.findAllBySaleOrderId(saleOrderId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderItemResponse> fetchByClientId(Long clientId, Pageable pageable) {
        log.info("Fetch sale order items by client id {}", clientId);

        return repository.findByClientId(clientId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderItemResponse> fetchByuserId(Long userId, Pageable pageable) {
        log.info("Fetch sale order items by user id {}", userId);
        return repository.findByUserId(userId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderItemResponse> fetchByWarehouseId(Long warehouseId, Pageable pageable) {
        log.info("Fetch sale order items by warehouse id {}", warehouseId);

        return repository.findByWarehouseId(warehouseId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderItemResponse> fetchByGoodsId(Long goodsId, Pageable pageable) {
        log.info("Fetch sale order items by goods id {}", goodsId);

        return repository.findByGoodsId(goodsId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public List<SaleOrderItemResponse> fetchByArrivalDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetch sale order items by arrival date range {} - {}", startDate, endDate);

        return repository.findByArrivalDateBetween(startDate, endDate)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "SaleOrderItem"
    )
//    @CacheEvict(value = {"saleOrderItems", "saleOrderItem"}, allEntries = true)
    @Transactional
    public SaleOrderItemResponse update(Long id, SaleOrderItemDTO dto) {
        log.info("Update sale order item with id {}", id);

        SaleOrderItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order item not found with id: " + id));
        validateSaleOrderStatus(entity.getSaleOrder());

        Goods oldGoods = entity.getGoods();
        BigDecimal oldCount = entity.getCount();
        Long warehouseId = entity.getWarehouse().getId();

        Goods newGoods = oldGoods;
        if (dto.getGoodsId() != null && !dto.getGoodsId().equals(oldGoods.getId())) {
            newGoods = goodsRepository.findById(dto.getGoodsId())
                    .orElseThrow(() -> new ResourceNotFoundException("Goods not found with id: " + dto.getGoodsId()));
        }
        boolean goodsChanged = newGoods != oldGoods;

        // WINDOW uchun dto.count - dona (bo'laklar soni), entity.count esa kv.m.
        // Partial update'da kelmagan qiymatlar mavjud pozitsiyadan olinadi.
        BigDecimal width = dto.getWidth() != null ? dto.getWidth() : entity.getWidth();
        BigDecimal height = dto.getHeight() != null ? dto.getHeight() : entity.getHeight();
        BigDecimal requested = dto.getCount();
        if (requested == null) {
            requested = !goodsChanged && oldGoods.getType() == Type.WINDOW
                    ? windowPieces(entity)
                    : oldCount;
        }
        BigDecimal newCount = resolveCount(newGoods, width, height, requested);

        boolean countChanged = oldCount == null || oldCount.compareTo(newCount) != 0;
        boolean stockChanged = goodsChanged || countChanged;

        if (stockChanged) {
            log.info("Sale order item {} changed (goods {} -> {}, count {} -> {}). Returning old quantity to stock.",
                    id, oldGoods.getId(), newGoods.getId(), oldCount, newCount);
            returnToStock(entity);
            if (newGoods.getType() != Type.SERVICE) {
                validateAndCheckStock(warehouseId, newGoods.getId(), newCount);
            }
        }

        mapper.updateEntity(entity, dto);
        entity.setGoods(newGoods);
        entity.setCount(newCount);
        if (newGoods.getType() == Type.WINDOW) {
            entity.setWidth(width);
            entity.setHeight(height);
        }

        entity = repository.save(entity);

        if (stockChanged && newGoods.getType() != Type.SERVICE) {
            stockService.decreaseStock(newGoods.getId(), warehouseId, newCount, sheetPieces(newGoods, newCount));
        }

        return mapper.toResponse(entity);
    }

    @Override
    public void moveItemsToWarehouse(Long saleOrderId, Warehouse target) {
        for (SaleOrderItem item : repository.findAllBySaleOrderId(saleOrderId)) {
            if (item.getStatus() != Status.ACTIVE) {
                continue;
            }
            if (item.getWarehouse() != null && item.getWarehouse().getId().equals(target.getId())) {
                continue;
            }
            Goods goods = item.getGoods();
            boolean tracked = goods != null && goods.getType() != Type.SERVICE && item.getCount() != null;
            if (tracked) {
                returnToStock(item);
                validateAndCheckStock(target.getId(), goods.getId(), item.getCount());
            }
            item.setWarehouse(target);
            repository.save(item);
            if (tracked) {
                stockService.decreaseStock(goods.getId(), target.getId(), item.getCount(), sheetPieces(goods, item.getCount()));
            }
        }
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "SaleOrderItem"
    )
//    @CacheEvict(value = {"saleOrderItems", "saleOrderItem"}, allEntries = true)
    public void delete(Long id) {
        log.info("Delete sale order item with id {}", id);

        SaleOrderItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order item not found with id: " + id));

        validateSaleOrderStatus(entity.getSaleOrder());
        entity.setStatus(Status.DELETED);
        repository.save(entity);
        returnToStock(entity);
    }

    @Override
    public void returnItemsToStock(Long saleOrderId) {
        for (SaleOrderItem item : repository.findAllBySaleOrderId(saleOrderId)) {
            if (item.getStatus() == Status.ACTIVE) {
                returnToStock(item);
            }
        }
    }

    private void returnToStock(SaleOrderItem item) {
        Goods goods = item.getGoods();
        if (goods == null || goods.getType() == Type.SERVICE || item.getCount() == null) {
            return;
        }
        stockService.increaseStock(goods.getId(), item.getWarehouse().getId(), item.getCount(),
                sheetPieces(goods, item.getCount()));
    }

    /**
     * Stock.pieceCount WINDOW uchun butun listlar sonini bildiradi (count esa kv.m).
     * Shuning uchun kv.m listlarga mahsulotning list o'lchami orqali o'tkaziladi. O'lcham
     * noma'lum bo'lsa null qaytadi va StockService mavjud nisbat bo'yicha proporsional hisoblaydi.
     */
    private BigDecimal sheetPieces(Goods goods, BigDecimal count) {
        return StockPieces.derive(goods, count);
    }

    private BigDecimal windowPieces(SaleOrderItem item) {
        BigDecimal w = item.getWidth();
        BigDecimal h = item.getHeight();
        if (w == null || h == null || w.signum() <= 0 || h.signum() <= 0 || item.getCount() == null) {
            return item.getCount();
        }
        BigDecimal pieceArea = w.multiply(h).divide(BigDecimal.valueOf(10_000), 6, java.math.RoundingMode.HALF_UP);
        return item.getCount().divide(pieceArea, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * ⭐ WINDOW (Oyna) turi uchun count'ni width * height (kv.m) sifatida hisoblaydi.
     * Boshqa turlar (PRODUCT / SERVICE) uchun DTO'da kelgan count o'zgarishsiz qaytariladi.
     *
     * @throws BadRequestException agar WINDOW uchun width/height berilmagan yoki noldan kichik bo'lsa
     */
    private BigDecimal resolveCount(Goods goods, BigDecimal width, BigDecimal height, BigDecimal dtoCount) {
        if (goods.getType() != Type.WINDOW) {
            return dtoCount;
        }
        if (width == null || height == null) {
            throw new BadRequestException("Oyna (WINDOW) uchun eni va bo‘yi majburiy!");
        }
        if (width.compareTo(BigDecimal.ZERO) <= 0 || height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Eni va bo‘yi noldan katta bo‘lishi kerak!");
        }
        if (dtoCount == null || dtoCount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Soni noldan katta bo‘lishi kerak!");
        }
        // sm → m²: (eni_sm * boyi_sm * dona) / 10000
        BigDecimal kvm = width
                .multiply(height)
                .multiply(dtoCount)
                .divide(BigDecimal.valueOf(10_000), StockPieces.SCALE, java.math.RoundingMode.HALF_UP);
        log.info("WINDOW kv.m: ({}sm * {}sm * {}) / 10000 = {}", width, height, dtoCount, kvm);
        return kvm;
    }

    /**
     * ⭐ WAREHOUSE STOCK VALIDATION METODI ⭐
     * Bu metod warehouse'dagi mahsulot stokunni tekshiradi
     * Agar warehouse'da 10 bor leki client 12 ta kirgizmonga harakat qilsa - XATOLIK!
     *
     * Eslatma: SERVICE turidagi Goods uchun bu metod chaqirilmaydi (create/update
     * metodlarida isService flag orqali oldindan filtr qilinadi).
     *
     * @throws InsufficientStockException agar stock yetarli bo'lmasa
     */
    @Override
    public void validateAndCheckStock(Long warehouseId, Long goodsId, BigDecimal requestedCount) {
        log.info("Validating warehouse stock. Warehouse: {}, Goods: {}, Requested: {}",
                warehouseId, goodsId, requestedCount);

        // Manfiy yoki nol qiymat check
        if (requestedCount == null || requestedCount.compareTo(BigDecimal.ZERO) <= 0) {
            String errorMsg = String.format(
                    "Mahsulot soni nol'dan katta bo'lishi kerak! Kirgizilgan: %s",
                    requestedCount
            );
            log.error(errorMsg);
            throw new InsufficientStockException(errorMsg);
        }

        // Warehouse'dagi mavjud stockni Stock jadvalidan (haqiqiy manba) olamiz
        BigDecimal availableStock = stockService.getAvailableStock(goodsId, warehouseId);

        log.info("Stock check: available={}, requested={}", availableStock, requestedCount);

        // Agar mavjud stock kirgizilgan sonidan kam bo'lsa - XATOLIK!
        if (availableStock.compareTo(requestedCount) < 0) {
            BigDecimal shortage = requestedCount.subtract(availableStock);
            String errorMsg = String.format(
                    "Ombareda yetarli mahsulot yo'q! Mavjud: %s, Talabalar: %s, Kamiy: %s",
                    availableStock,
                    requestedCount,
                    shortage
            );
            log.error(errorMsg);
            throw new InsufficientStockException(
                    errorMsg,
                    goodsId,
                    warehouseId,
                    availableStock,
                    requestedCount
            );
        }

        log.info("Stock validation passed successfully");
    }

    @Override
    public Page<SaleOrderItemResponse> fetchByGoodsTypeAndDateRange(
            Type type,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Filter sale order items by goods type {} and date range {} - {}", type, startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return repository.findByGoodsTypeAndArrivalDateBetween(type, start, end, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public void validateSaleOrderStatus(SaleOrder saleOrder) {
        if (saleOrder == null) {
            throw new BadRequestException("Sale Order is required.");
        }

        if (saleOrder.getSalesOrderStatus() == null) {
            throw new BadRequestException("Sale Order status is not defined.");
        }

        if (saleOrder.getSalesOrderStatus().isFinal()) {
            throw new BadRequestException(
                    String.format(
                            "Sale Order is %s. You cannot add, update or delete items.",
                            saleOrder.getSalesOrderStatus()
                    )
            );
        }
    }

}