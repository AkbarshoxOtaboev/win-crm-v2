package uz.script.wincrm.warehouse.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.sms.SmsSendException;
import uz.script.wincrm.sms.SmsService;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.suppliers.Supplier;
import uz.script.wincrm.suppliers.repository.SupplierRepository;
import uz.script.wincrm.suppliers.service.SupplierBalanceService;
import uz.script.wincrm.telegram.TelegramSendResult;
import uz.script.wincrm.telegram.config.TelegramBotLifecycleService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.WarehouseOrder;
import uz.script.wincrm.warehouse.WarehouseOrderItem;
import uz.script.wincrm.warehouse.dto.WarehouseOrderDTO;
import uz.script.wincrm.warehouse.enums.WarehouseOrderStatus;
import uz.script.wincrm.warehouse.mapper.WarehouseOrderMapper;
import uz.script.wincrm.warehouse.repository.WarehouseOrderItemRepository;
import uz.script.wincrm.warehouse.repository.WarehouseOrderRepository;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;
import uz.script.wincrm.warehouse.response.WarehouseOrderResponse;
import uz.script.wincrm.warehouse.service.WarehouseOrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WarehouseOrderServiceImpl implements WarehouseOrderService {

    private final WarehouseOrderRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseOrderMapper mapper;
    private final SupplierBalanceService supplierBalanceService;
    private final WarehouseOrderItemRepository warehouseOrderItemRepository;
    private final StockService stockService;
    private final SmsService smsService;
    private final TelegramBotLifecycleService telegramBotLifecycleService;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "WarehouseOrder"
    )
//    @CacheEvict(value = "warehouseOrders", allEntries = true)
    public WarehouseOrderResponse create(WarehouseOrderDTO dto) {
        log.info("Create warehouse order");

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        WarehouseOrder order = mapper.toEntity(dto, supplier, warehouse);
        order.setCreatedUsername(username);
        Currency currency = dto.getCurrency() != null ? dto.getCurrency() : Currency.BASE;
        order.setCurrency(currency);
        order.setExchangeRate(currency.isBase() || dto.getExchangeRate() == null
                ? resolveRate(currency, dto.getArrivalDate())
                : dto.getExchangeRate());

        // Order yaratilganda hali item yo'q, totalSum = 0/null.
        // Supplier balansi (totalPurchase/totalDebt) faqat item qo'shilganda
        // WarehouseOrderItemServiceImpl#recalculateOrderTotalSum orqali yangilanadi.
        order = repository.save(order);

        BigDecimal serviceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
        if (serviceFee.compareTo(BigDecimal.ZERO) > 0) {
            supplierBalanceService.increasePurchase(supplier.getId(), currency, serviceFee);
            log.info("Supplier balance increased by service fee. SupplierId: {}, Fee: {} {}",
                    supplier.getId(), serviceFee, currency);
        }

        return mapper.toResponse(order);
    }

    @Override
//    @Cacheable(value = "warehouseOrder", key = "#id")
    public WarehouseOrderResponse findById(Long id) {
        log.info("Fetch warehouse order by id {}", id);

        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Warehouse order not found with id: " + id));

        return mapper.toResponse(order);
    }

    @Override
//    @Cacheable(value = "warehouseOrders")
    public List<WarehouseOrderResponse> fetchAllOrders() {
        log.info("Fetch all warehouse orders");

        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
//    @Cacheable(value = "warehouseOrdersByWarehouse", key = "#warehouseId")
    public List<WarehouseOrderResponse> fetchByWarehouseId(Long warehouseId) {
        log.info("Fetch warehouse orders by warehouse id {}", warehouseId);

        return repository.findAllByWarehouseId(warehouseId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
//    @Cacheable(value = "warehouseOrdersBySupplier", key = "#supplierId")
    public List<WarehouseOrderResponse> fetchBySupplierId(Long supplierId) {
        log.info("Fetch warehouse orders by supplier id {}", supplierId);

        return repository.findAllBySupplierId(supplierId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "WarehouseOrder"
    )
//    @CacheEvict(value = {"warehouseOrders", "warehouseOrder", "warehouseOrdersByWarehouse", "warehouseOrdersBySupplier"}, allEntries = true)
    public WarehouseOrderResponse update(Long id, WarehouseOrderDTO dto) {
        log.info("Update warehouse order with id {}", id);

        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Warehouse order not found with id: " + id));

        Supplier newSupplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Supplier not found with id: " + dto.getSupplierId()));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        // WarehouseOrderDTO totalSum saqlamaydi (u item'lardan avtomatik hisoblanadi),
        // shuning uchun bu yerda faqat supplier/valyuta o'zgarganda balansni ko'chiramiz.
        Long oldSupplierId = order.getSupplier().getId();
        Long newSupplierId = newSupplier.getId();
        Currency oldCurrency = order.getCurrency();
        BigDecimal currentTotalSum = order.getTotalSum() != null ? order.getTotalSum() : BigDecimal.ZERO;

        BigDecimal oldServiceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;

        List<WarehouseOrderItem> activeItems = warehouseOrderItemRepository.findAllByWarehouseOrderId(id)
                .stream()
                .filter(i -> i.getStatus() == Status.ACTIVE)
                .toList();

        Currency newCurrency = dto.getCurrency() != null ? dto.getCurrency() : oldCurrency;
        if (newCurrency != oldCurrency && !activeItems.isEmpty()) {
            throw new BadRequestException("Pozitsiyalar kiritilgan hujjat valyutasini o'zgartirib bo'lmaydi. "
                    + "Avval pozitsiyalarni o'chiring yoki yangi hujjat yarating.");
        }
        boolean transferred = order.getOrderStatus() == WarehouseOrderStatus.TRANSFERRED;
        boolean arrivalDayChanged = dto.getArrivalDate() != null && order.getArrivalDate() != null
                && !dto.getArrivalDate().toLocalDate().equals(order.getArrivalDate().toLocalDate());
        BigDecimal newRate;
        if (newCurrency != null && !newCurrency.isBase() && dto.getExchangeRate() != null) {
            newRate = dto.getExchangeRate();
        } else if (newCurrency != oldCurrency || (arrivalDayChanged && !transferred)) {
            newRate = resolveRate(newCurrency, dto.getArrivalDate() != null ? dto.getArrivalDate() : order.getArrivalDate());
        } else {
            newRate = order.getExchangeRate();
        }
        if (transferred && order.getExchangeRate().compareTo(newRate) != 0) {
            throw new BadRequestException("Omborga o'tkazilgan hujjat kursini o'zgartirib bo'lmaydi: "
                    + "ombordagi tannarx shu kurs bilan hisoblangan.");
        }

        mapper.updateEntity(order, dto, newSupplier, warehouse);
        order.setCurrency(newCurrency);
        order.setExchangeRate(newRate);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setCreatedUsername(username);

        BigDecimal itemsTotal = activeItems.stream()
                .map(i -> i.getPriceCost().multiply(i.getCount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newServiceFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
        BigDecimal newTotalSum = itemsTotal.add(newServiceFee);
        order.setTotalSum(newTotalSum);
        order = repository.save(order);

        if (!Objects.equals(oldSupplierId, newSupplierId) || newCurrency != oldCurrency) {
            if (currentTotalSum.signum() != 0) {
                supplierBalanceService.decreasePurchase(oldSupplierId, oldCurrency, currentTotalSum);
            }
            if (newTotalSum.signum() != 0) {
                supplierBalanceService.increasePurchase(newSupplierId, newCurrency, newTotalSum);
            }
            log.info("Order balance moved. {} {} (-{}) -> {} {} (+{})",
                    oldSupplierId, oldCurrency, currentTotalSum, newSupplierId, newCurrency, newTotalSum);
        } else {
            BigDecimal totalDiff = newTotalSum.subtract(currentTotalSum);
            if (totalDiff.signum() > 0) {
                supplierBalanceService.increasePurchase(newSupplierId, newCurrency, totalDiff);
            } else if (totalDiff.signum() < 0) {
                supplierBalanceService.decreasePurchase(newSupplierId, newCurrency, totalDiff.abs());
            }
            if (totalDiff.signum() != 0) {
                log.info("Order total updated. SupplierId: {}, Diff: {} {}, ServiceFee: {} -> {}",
                        newSupplierId, totalDiff, newCurrency, oldServiceFee, newServiceFee);
            }
        }

        return mapper.toResponse(order);
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "WarehouseOrder"
    )
//    @CacheEvict(value = {"warehouseOrders", "warehouseOrder", "warehouseOrdersByWarehouse", "warehouseOrdersBySupplier"}, allEntries = true)
    public void delete(Long id) {
        log.info("Delete warehouse order with id {}", id);

        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse order not found with id: " + id));

        if (order.getOrderStatus() == WarehouseOrderStatus.TRANSFERRED) {
            List<WarehouseOrderItem> items = warehouseOrderItemRepository.findAllByWarehouseOrderId(id);
            for (WarehouseOrderItem item : items) {
                if (item.getStatus() == Status.ACTIVE) {
                    BigDecimal pieces = item.getPieceCount() != null ? item.getPieceCount() : item.getCount();
                    stockService.decreaseStock(
                            item.getGoods().getId(),
                            item.getWarehouse().getId(),
                            item.getCount(),
                            pieces);
                }
            }
            log.info("Order was TRANSFERRED, stock reverted for {} items", items.size());
        }

        order.setStatus(Status.DELETED);
        repository.save(order);

        BigDecimal totalSum = order.getTotalSum() != null ? order.getTotalSum() : BigDecimal.ZERO;
        if (totalSum.compareTo(BigDecimal.ZERO) != 0) {
            supplierBalanceService.decreasePurchase(order.getSupplier().getId(), order.getCurrency(), totalSum);
            log.info("Supplier balance decreased by purchase. SupplierId: {}, Sum: {}",
                    order.getSupplier().getId(), totalSum);
        }
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "WarehouseOrder")
    public WarehouseOrderResponse transferToWarehouse(Long id) {
        log.info("Transfer warehouse order to stock, id {}", id);

        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse order not found with id: " + id));

        if (order.getOrderStatus() == WarehouseOrderStatus.TRANSFERRED) {
            throw new BadRequestException("Order allaqachon omborga transfer qilingan: " + id);
        }

        List<WarehouseOrderItem> items = warehouseOrderItemRepository.findAllByWarehouseOrderId(id)
                .stream()
                .filter(i -> i.getStatus() == Status.ACTIVE)
                .toList();

        if (items.isEmpty()) {
            throw new BadRequestException("Item'lari yo'q orderni transfer qilib bo'lmaydi: " + id);
        }

        for (WarehouseOrderItem item : items) {
            BigDecimal pieces = item.getPieceCount() != null ? item.getPieceCount() : item.getCount();
            stockService.increaseStock(
                    item.getGoods().getId(),
                    item.getWarehouse().getId(),
                    item.getCount(),
                    pieces,
                    order.toBase(item.getPriceCost()));
        }

        order.setOrderStatus(WarehouseOrderStatus.TRANSFERRED);
        order = repository.save(order);

        log.info("Order transferred to stock. OrderId: {}, items: {}", id, items.size());
        return mapper.toResponse(order);
    }

    private BigDecimal resolveRate(Currency currency, LocalDateTime arrivalDate) {
        if (currency == null || currency.isBase()) {
            return BigDecimal.ONE;
        }
        return exchangeRateService.rateOn(currency, arrivalDate != null ? arrivalDate.toLocalDate() : LocalDate.now());
    }

    @Override
    public void sendSmsToSupplier(Long id, String message) {
        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse order not found with id: " + id));
        Supplier supplier = order.getSupplier();
        if (supplier == null || supplier.getPhone() == null || supplier.getPhone().isBlank()) {
            throw new BadRequestException("Yetkazuvchi telefon raqami topilmadi");
        }
        try {
            smsService.sendSms(supplier.getPhone(), message);
            log.info("Warehouse order SMS sent. OrderId: {}, SupplierId: {}", id, supplier.getId());
        } catch (SmsSendException e) {
            throw new BadRequestException("SMS yuborilmadi: " + e.getMessage());
        }
    }

    @Override
    public void sendTelegramToSupplier(Long id, String message) {
        WarehouseOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse order not found with id: " + id));
        Supplier supplier = order.getSupplier();
        if (supplier == null || supplier.getPhone() == null || supplier.getPhone().isBlank()) {
            throw new BadRequestException("Yetkazuvchi telefon raqami topilmadi");
        }

        TelegramSendResult result = telegramBotLifecycleService.sendMessageToPhone(supplier.getPhone(), message);
        switch (result) {
            case SENT -> log.info("Warehouse order Telegram sent. OrderId: {}, SupplierId: {}", id, supplier.getId());
            case BOT_NOT_CONNECTED -> throw new BadRequestException(
                    "Telegram bot hozircha ulanmagan. Admin panelidan bot tokenini tekshiring.");
            case CLIENT_NOT_LINKED -> throw new BadRequestException(
                    "Yetkazuvchi Telegram botdan hali ro'yxatdan o'tmagan (telefon raqami orqali /start bosilmagan).");
            case SEND_FAILED -> throw new BadRequestException(
                    "Xabar yuborishda Telegram API xatoligi yuz berdi. Qaytadan urinib ko'ring.");
        }
    }
}