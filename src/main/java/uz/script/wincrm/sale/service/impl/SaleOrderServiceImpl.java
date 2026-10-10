package uz.script.wincrm.sale.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.clients.repository.ClientRepository;
import uz.script.wincrm.clients.service.ClientBalanceService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.discount.service.DiscountRuleService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.kpi.service.KpiService;
import uz.script.wincrm.salary.service.SalaryCommissionService;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.payment.Payment;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.production.repository.ProductionOrderRepository;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.dto.ApplyDiscountDTO;
import uz.script.wincrm.sale.dto.SaleOrderDTO;
import uz.script.wincrm.sale.dto.SaleOrderDiscountHistoryDTO;
import uz.script.wincrm.sale.dto.SaleOrderHistoryDTO;
import uz.script.wincrm.sale.dto.SaleOrderItemDTO;
import uz.script.wincrm.sale.enums.DeliveryType;
import uz.script.wincrm.sale.enums.SaleType;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.mapper.SaleOrderDiscountHistoryMapper;
import uz.script.wincrm.sale.mapper.SaleOrderMapper;
import uz.script.wincrm.sale.repository.SaleOrderDiscountHistoryRepository;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.response.SaleOrderDiscountHistoryResponse;
import uz.script.wincrm.sale.response.SaleOrderResponse;
import uz.script.wincrm.sale.service.DiscountCalculator;
import uz.script.wincrm.sale.service.DiscountLimitPolicy;
import uz.script.wincrm.sale.service.SaleOrderDiscountHistoryRecorder;
import uz.script.wincrm.sale.service.SaleOrderHistoryService;
import uz.script.wincrm.sale.service.SaleOrderItemService;
import uz.script.wincrm.sale.service.SaleOrderService;
import uz.script.wincrm.production.service.ProductionOrderService;
import uz.script.wincrm.transport.service.TransportDeliveryService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleOrderServiceImpl implements SaleOrderService {

    private final SaleOrderRepository repository;
    private final ProductionOrderRepository productionOrderRepository;
    private final PaymentRepository paymentRepository;
    private final SaleOrderMapper mapper;
    private final ClientRepository clientRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final ClientBalanceService clientBalanceService;
    private final SaleOrderHistoryService saleOrderHistoryService;
    private final SaleOrderItemService saleOrderItemService;
    private final ProductionOrderService productionOrderService;
    private final TransportDeliveryService transportDeliveryService;

    // --- chegirma uchun qo'shilgan bog'liqliklar ---
    private final DiscountCalculator discountCalculator;
    private final SaleOrderDiscountHistoryRecorder discountHistoryRecorder;
    private final SaleOrderDiscountHistoryRepository discountHistoryRepository;
    private final SaleOrderDiscountHistoryMapper discountHistoryMapper;
    private final DiscountLimitPolicy discountLimitPolicy;
    private final DiscountRuleService discountRuleService;
    private final KpiService kpiService;
    private final SalaryCommissionService salaryCommissionService;
    private final ExchangeRateService exchangeRateService;
    private final SaleOrderItemRepository saleOrderItemRepository;

    private static final Set<String> SELLER_OVERRIDE_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "SaleOrder"
    )
    public SaleOrderResponse create(SaleOrderDTO dto) {
        log.info("Create sale order");

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));

        Client client = null;
        if (dto.getClientId() != null) {
            client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.getClientId()));
        }

        User currentUser = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
        assertCanSellAs(currentUser);

        SaleType saleType = dto.getSaleType() != null ? dto.getSaleType() : SaleType.RETAIL;

        SaleOrder entity = mapper.toEntity(dto);
        entity.setSaleType(saleType);
        entity.setClient(client);
        entity.setWarehouse(warehouse);
        entity.setUser(currentUser);
        entity.setStatus(Status.ACTIVE);
        entity.setSalesOrderStatus(SalesOrderStatus.NEW);
        Currency currency = CurrencyMath.orBase(dto.getCurrency());
        entity.setCurrency(currency);
        entity.setExchangeRate(currency.isBase() || dto.getExchangeRate() == null
                ? resolveRate(currency, dto.getOrderDate())
                : dto.getExchangeRate());

        // Kelgan summa - ASL summa (itemlardan yig'ilganmi yoki to'g'ridan-to'g'ri - farqi yo'q)
        BigDecimal original = dto.getTotalSum();
        if (original.signum() < 0) {
            throw new BadRequestException("Buyurtma summasi manfiy bo'lishi mumkin emas");
        }
        entity.setOriginalTotalSum(original);
        applyDelivery(entity, dto.getDeliveryType(), dto.getDeliveryFee());

        // Boshlang'ich holatda chegirma yo'q. Agar create paytida chegirma kelsa, quyida qo'llanadi.
        entity.setDiscountAmount(BigDecimal.ZERO);
        recalculateTotals(entity);

        entity = repository.save(entity);

        saleOrderHistoryService.recordHistory(
                SaleOrderHistoryDTO.builder()
                        .saleOrderId(entity.getId())
                        .fromStatus(null)
                        .toStatus(SalesOrderStatus.NEW)
                        .build()
        );

        createInitialItems(entity, dto);

        // Create paytida ixtiyoriy chegirma - pozitsiyalardan keyin, mahsulot chegirma chegarasi tekshirilishi uchun
        if (dto.getDiscountType() != null && dto.getDiscountValue() != null) {
            applyDiscountInternal(entity, dto.getDiscountType(), dto.getDiscountValue(), true);
            entity = repository.save(entity);
        }

        if (saleType == SaleType.WHOLESALE) {
            entity.setSalesOrderStatus(SalesOrderStatus.WORK_DONE);
            entity = repository.save(entity);
            saleOrderHistoryService.recordHistory(
                    SaleOrderHistoryDTO.builder()
                            .saleOrderId(entity.getId())
                            .fromStatus(SalesOrderStatus.NEW)
                            .toStatus(SalesOrderStatus.WORK_DONE)
                            .comment("Optom sotuv: tovar joyida topshirildi")
                            .build()
            );
        }

        if (entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }

        return mapper.toResponse(entity);
    }

    /** SUPER_ADMIN/ADMIN/DIRECTOR istalgan sotuvchi nomidan, qolganlar faqat o'z nomidan sotuv yaratadi. */
    private void assertCanSellAs(User seller) {
        User actor = getCurrentUser();
        if (actor.getId().equals(seller.getId())) {
            return;
        }
        boolean elevated = actor.getRoles() != null && actor.getRoles().stream()
                .anyMatch(r -> SELLER_OVERRIDE_ROLES.contains(r.getName()));
        if (!elevated) {
            throw new ForbiddenException("Siz faqat o'zingiz nomingizdan sotuv yarata olasiz");
        }
    }

    private void createInitialItems(SaleOrder order, SaleOrderDTO dto) {
        List<SaleOrderItemDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        if (order.getClient() == null) {
            throw new BadRequestException("Pozitsiyali buyurtma uchun mijoz majburiy");
        }
        for (SaleOrderItemDTO item : items) {
            if (item == null || item.getGoodsId() == null) {
                throw new BadRequestException("Pozitsiyada mahsulot tanlanmagan");
            }
            if (item.getCount() == null || item.getCount().signum() <= 0) {
                throw new BadRequestException("Pozitsiya soni noldan katta bo'lishi kerak");
            }
            if (item.getPriceSelling() == null || item.getPriceSelling().signum() <= 0) {
                throw new BadRequestException("Pozitsiya sotish narxi noldan katta bo'lishi kerak");
            }
            if (item.getPriceCost() == null) {
                item.setPriceCost(BigDecimal.ZERO);
            } else if (item.getPriceCost().signum() < 0) {
                throw new BadRequestException("Pozitsiya tannarxi manfiy bo'lishi mumkin emas");
            }
            item.setSaleOrderId(order.getId());
            item.setClientId(order.getClient().getId());
            item.setWarehouseId(order.getWarehouse().getId());
            if (item.getArrivalDate() == null) {
                item.setArrivalDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());
            }
            saleOrderItemService.create(item);
        }
    }

    @Override
    public SaleOrderResponse findById(Long id) {
        log.info("Fetch sale order by id {}", id);

        SaleOrder entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<SaleOrderResponse> fetchAll(Pageable pageable) {
        log.info("Fetch all sale orders");

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderResponse> fetchByClientId(Long clientId, Pageable pageable) {
        log.info("Fetch sale orders by client id {}", clientId);

        return repository.findByClientId(clientId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderResponse> fetchByWarehouseId(Long warehouseId, Pageable pageable) {
        log.info("Fetch sale orders by warehouse id {}", warehouseId);

        return repository.findByWarehouseId(warehouseId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<SaleOrderResponse> fetchByUserId(Long userId, Pageable pageable) {
        log.info("Fetch sale orders by user id {}", userId);

        return repository.findByUserId(userId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public List<SaleOrderResponse> fetchByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetch sale orders by date range {} - {}", startDate, endDate);

        return repository.findByOrderDateBetween(startDate, endDate)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "SaleOrder"
    )
    public SaleOrderResponse update(Long id, SaleOrderDTO dto) {
        log.info("Update sale order with id {}", id);

        SaleOrder entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        ensureFinalOrderUnchanged(entity, dto);

        if (dto.getWarehouseId() != null && !dto.getWarehouseId().equals(entity.getWarehouse().getId())) {
            if (entity.getSalesOrderStatus().isFinal()) {
                throw new BadRequestException(
                        "Yakuniy holatdagi (" + entity.getSalesOrderStatus() + ") buyurtmaning omborini o'zgartirib bo'lmaydi");
            }
            Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + dto.getWarehouseId()));
            saleOrderItemService.moveItemsToWarehouse(entity.getId(), warehouse);
            entity.setWarehouse(warehouse);
        }

        Long previousClientId = entity.getClient() != null ? entity.getClient().getId() : null;
        if (dto.getClientId() != null && (entity.getClient() == null || !dto.getClientId().equals(entity.getClient().getId()))) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.getClientId()));
            entity.setClient(client);
        }

        LocalDateTime previousOrderDate = entity.getOrderDate();
        mapper.updateEntity(entity, dto);
        applyCurrencyChange(entity, dto, previousOrderDate);

        if (dto.getDeliveryType() != null) {
            applyDelivery(entity, dto.getDeliveryType(), dto.getDeliveryFee());
        }

        // Agar update'da totalSum o'zgargan bo'lsa - u yangi ASL summa bo'ladi.
        // Mavjud chegirmani yangi asl summaga qayta qo'llaymiz, aks holda totalSum noto'g'ri qoladi.
        if (dto.getTotalSum() != null) {
            if (dto.getTotalSum().signum() < 0) {
                throw new BadRequestException("Buyurtma summasi manfiy bo'lishi mumkin emas");
            }
            entity.setOriginalTotalSum(dto.getTotalSum());
        }
        if (dto.getTotalSum() != null && entity.getDiscountType() != null && entity.getDiscountValue() != null) {
            applyDiscountInternal(entity, entity.getDiscountType(), entity.getDiscountValue(), false);
        } else {
            recalculateTotals(entity);
        }

        entity = repository.save(entity);

        Long currentClientId = entity.getClient() != null ? entity.getClient().getId() : null;
        if (currentClientId != null) {
            clientBalanceService.recalculateClientBalance(currentClientId);
        }
        if (previousClientId != null && !previousClientId.equals(currentClientId)) {
            clientBalanceService.recalculateClientBalance(previousClientId);
        }

        return mapper.toResponse(entity);
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "SaleOrder"
    )
    public void delete(Long id) {
        log.info("Delete sale order with id {}", id);

        SaleOrder entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        SalesOrderStatus current = entity.getSalesOrderStatus();
        if (current != SalesOrderStatus.CANCELLED
                && current != SalesOrderStatus.DELIVERED
                && current != SalesOrderStatus.WORK_DONE
                && current != SalesOrderStatus.COMPLETED) {
            saleOrderItemService.returnItemsToStock(id);
            productionOrderService.cancelForSaleOrder(id);
            transportDeliveryService.cancelForSaleOrder(id);
        }

        entity.setStatus(Status.DELETED);
        repository.saveAndFlush(entity);
        kpiService.removeForSaleOrder(id);
        salaryCommissionService.recalculateCommissionForSaleOrder(entity);

        if (entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "SaleOrder"
    )
    public void changeStatus(Long id, SalesOrderStatus salesOrderStatus, String comment) {
        log.info("Change order {} status to {}", id, salesOrderStatus);
        SaleOrder entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        SalesOrderStatus currentStatus = entity.getSalesOrderStatus();

        if (!currentStatus.canTransitionTo(salesOrderStatus)) {
            throw new BadRequestException(
                    "Cannot change order status from " + currentStatus + " to " + salesOrderStatus);
        }

        String trimmedComment = comment != null ? comment.trim() : null;
        if (salesOrderStatus == SalesOrderStatus.CANCELLED
                && (trimmedComment == null || trimmedComment.isEmpty())) {
            throw new BadRequestException("Buyurtmani bekor qilish sababini (izoh) kiriting");
        }

        if (salesOrderStatus == SalesOrderStatus.COMPLETED) {
            BigDecimal paid = paymentRepository.findBySaleOrderId(id).stream()
                    .map(Payment::appliedOrPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal debt = entity.getTotalSum().subtract(paid);
            if (debt.compareTo(CurrencyMath.TOLERANCE) >= 0) {
                throw new BadRequestException(
                        "Buyurtma bo'yicha " + CurrencyMath.format(debt, entity.currencyOrBase())
                                + " qarz bor. Yakunlash uchun avval qarz to'liq to'lanishi kerak.");
            }
        }

        if (salesOrderStatus == SalesOrderStatus.PROCESSING
                && !productionOrderRepository.existsBySaleOrder_Id(id)) {
            throw new BadRequestException(
                    "Ishlab chiqarishga yuborish uchun sex tanlang (send-to-production)");
        }

        if (salesOrderStatus == SalesOrderStatus.READY
                || salesOrderStatus == SalesOrderStatus.IN_DELIVERY
                || salesOrderStatus == SalesOrderStatus.DELIVERED) {
            productionOrderRepository.findBySaleOrder_Id(id).ifPresent(po -> {
                if (po.getProductionStatus() != uz.script.wincrm.production.enums.ProductionOrderStatus.DONE) {
                    throw new BadRequestException(
                            "Ishlab chiqarish yakunlanmagan. Avval sexda complete qiling.");
                }
            });
        }

        if (salesOrderStatus == SalesOrderStatus.IN_DELIVERY) {
            if (entity.getDeliveryType() == DeliveryType.PICKUP) {
                throw new BadRequestException(
                        "Mijoz buyurtmani o'zi olib ketadi - transportga yuborib bo'lmaydi. Buyurtmani tahrirlab yetkazib berish xizmatini tanlang.");
            }
            transportDeliveryService.createForSaleOrder(entity);
        }

        if (currentStatus == SalesOrderStatus.IN_DELIVERY && salesOrderStatus == SalesOrderStatus.DELIVERED) {
            transportDeliveryService.confirmForSaleOrder(entity);
        }

        if (salesOrderStatus == SalesOrderStatus.CANCELLED) {
            saleOrderItemService.returnItemsToStock(id);
            productionOrderService.cancelForSaleOrder(id);
            transportDeliveryService.cancelForSaleOrder(id);
        }

        entity.setSalesOrderStatus(salesOrderStatus);
        if (salesOrderStatus == SalesOrderStatus.CANCELLED) {
            entity.setDebtSum(BigDecimal.ZERO);
        }
        repository.saveAndFlush(entity);

        saleOrderHistoryService.recordHistory(
                SaleOrderHistoryDTO.builder()
                        .saleOrderId(entity.getId())
                        .fromStatus(currentStatus)
                        .toStatus(salesOrderStatus)
                        .comment(trimmedComment != null && !trimmedComment.isEmpty() ? trimmedComment : null)
                        .build()
        );

        if (salesOrderStatus == SalesOrderStatus.CANCELLED && entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }

        if (salesOrderStatus == SalesOrderStatus.COMPLETED) {
            kpiService.accrueForSaleOrder(entity);
            salaryCommissionService.recalculateCommissionForSaleOrder(entity);
        }
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "SaleOrder"
    )
    public SaleOrderResponse applyDiscount(Long id, ApplyDiscountDTO dto) {
        log.info("Apply discount to sale order {}", id);

        SaleOrder entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        if (entity.getSalesOrderStatus().isFinal()) {
            throw new BadRequestException(
                    "Yakuniy holatdagi (" + entity.getSalesOrderStatus() + ") buyurtmaga chegirma qo'llab bo'lmaydi");
        }

        applyDiscountInternal(entity, dto.getDiscountType(), dto.getDiscountValue(), true);

        entity = repository.save(entity);

        if (entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }

        return mapper.toResponse(entity);
    }

    @Override
    public List<SaleOrderDiscountHistoryResponse> fetchDiscountHistory(Long id) {
        log.info("Fetch discount history for sale order {}", id);

        // buyurtma mavjudligini tekshirish
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + id));

        return discountHistoryRepository.findBySaleOrderIdOrderByCreatedAtAsc(id)
                .stream()
                .map(discountHistoryMapper::toResponse)
                .toList();
    }

    /**
     * Chegirmani entity'ga hisoblab yozadigan yagona ichki metod. FAQAT totalSum'ga ta'sir qiladi:
     * totalSum = originalTotalSum - discountAmount. SaleOrderItem hisob-kitobi aralashmaydi.
     * Hisoblangandan keyin chegirma tarixiga (REQUIRES_NEW) yozuv tushadi.
     */
    private void applyDiscountInternal(SaleOrder entity,
                                       uz.script.wincrm.sale.enums.DiscountType type,
                                       BigDecimal value,
                                       boolean enforceGoodsLimit) {
        BigDecimal previousDiscount =
                entity.getDiscountAmount() != null ? entity.getDiscountAmount() : BigDecimal.ZERO;

        BigDecimal base = entity.getOriginalTotalSum();
        BigDecimal discountAmount = discountCalculator.calculate(base, type, value);
        if (enforceGoodsLimit) {
            discountRuleService.checkOrderDiscount(entity, discountAmount);
            discountLimitPolicy.checkOrderDiscount(entity, discountAmount);
        }

        entity.setDiscountType(type);
        entity.setDiscountValue(value);
        entity.setDiscountAmount(discountAmount);
        recalculateTotals(entity);

        discountHistoryRecorder.record(
                SaleOrderDiscountHistoryDTO.builder()
                        .saleOrderId(entity.getId())
                        .discountType(type)
                        .discountValue(value)
                        .discountAmount(discountAmount)
                        .previousDiscountAmount(previousDiscount)
                        .originalTotalSum(base)
                        .totalSumAfter(entity.getTotalSum())
                        .build()
        );
    }

    /**
     * Yakuniy (COMPLETED/CANCELLED) buyurtmada to'lov, KPI, sex va transport hisoblari yopilgan,
     * shuning uchun summaga ta'sir qiluvchi maydonlar o'zgarmaydi. Izoh va rejalashtirilgan sanalar ochiq.
     */
    private void ensureFinalOrderUnchanged(SaleOrder entity, SaleOrderDTO dto) {
        if (!entity.getSalesOrderStatus().isFinal()) {
            return;
        }
        BigDecimal original = entity.getOriginalTotalSum() != null ? entity.getOriginalTotalSum() : entity.getTotalSum();
        DeliveryType deliveryType = dto.getDeliveryType() != null ? dto.getDeliveryType() : entity.getDeliveryType();
        BigDecimal requestedFee = deliveryType == DeliveryType.DELIVERY
                ? (dto.getDeliveryFee() != null ? dto.getDeliveryFee() : entity.getDeliveryFee())
                : BigDecimal.ZERO;
        Long clientId = entity.getClient() != null ? entity.getClient().getId() : null;
        boolean changed =
                (dto.getTotalSum() != null && original != null && dto.getTotalSum().compareTo(original) != 0)
                        || (dto.getClientId() != null && !dto.getClientId().equals(clientId))
                        || (dto.getCurrency() != null && dto.getCurrency() != entity.currencyOrBase())
                        || deliveryType != entity.getDeliveryType()
                        || Objects.requireNonNullElse(requestedFee, BigDecimal.ZERO)
                        .compareTo(Objects.requireNonNullElse(entity.getDeliveryFee(), BigDecimal.ZERO)) != 0
                        || (dto.getOrderDate() != null && entity.getOrderDate() != null
                        && !dto.getOrderDate().withSecond(0).withNano(0)
                        .equals(entity.getOrderDate().withSecond(0).withNano(0)));
        if (changed) {
            throw new BadRequestException("Yakuniy holatdagi (" + entity.getSalesOrderStatus()
                    + ") buyurtmaning summasi, mijozi, valyutasi, yetkazib berishi va sanasini o'zgartirib bo'lmaydi");
        }
    }

    private BigDecimal resolveRate(Currency currency, LocalDateTime orderDate) {
        if (currency.isBase()) {
            return BigDecimal.ONE;
        }
        return exchangeRateService.rateOn(currency, orderDate != null ? orderDate.toLocalDate() : LocalDate.now());
    }

    /**
     * Pozitsiya narxlari va to'lovlar buyurtma valyutasida yoziladi, shuning uchun ular bor bo'lsa valyuta
     * o'zgarmaydi. Qo'lda kiritilgan kurs bo'lsa (yakuniy holatgacha) shu olinadi, aks holda buyurtma sanasidagi
     * Markaziy bank kursi: sana o'zgarsa yakuniy holatgacha qayta olinadi.
     */
    private void applyCurrencyChange(SaleOrder entity, SaleOrderDTO dto, LocalDateTime previousOrderDate) {
        Currency current = entity.currencyOrBase();
        Currency requested = dto.getCurrency() != null ? dto.getCurrency() : current;
        if (requested != current) {
            boolean hasItems = saleOrderItemRepository.findAllBySaleOrderId(entity.getId()).stream()
                    .anyMatch(i -> i.getStatus() == Status.ACTIVE);
            if (hasItems || !paymentRepository.findBySaleOrderId(entity.getId()).isEmpty()) {
                throw new BadRequestException("Pozitsiya yoki to'lov bor buyurtmaning valyutasini o'zgartirib bo'lmaydi");
            }
            entity.setCurrency(requested);
            entity.setExchangeRate(requested.isBase() || dto.getExchangeRate() == null
                    ? resolveRate(requested, entity.getOrderDate())
                    : dto.getExchangeRate());
            return;
        }
        if (!current.isBase() && dto.getExchangeRate() != null
                && dto.getExchangeRate().compareTo(entity.getExchangeRate()) != 0) {
            if (entity.getSalesOrderStatus().isFinal()) {
                throw new BadRequestException("Yakuniy holatdagi buyurtmaning kursini o'zgartirib bo'lmaydi");
            }
            entity.setExchangeRate(dto.getExchangeRate());
            return;
        }
        boolean dateChanged = entity.getOrderDate() != null && previousOrderDate != null
                && !entity.getOrderDate().toLocalDate().equals(previousOrderDate.toLocalDate());
        if (dateChanged && !current.isBase() && !entity.getSalesOrderStatus().isFinal()) {
            entity.setExchangeRate(resolveRate(current, entity.getOrderDate()));
        }
    }

    private void applyDelivery(SaleOrder entity, DeliveryType type, BigDecimal fee) {
        if (fee != null && fee.signum() < 0) {
            throw new BadRequestException("Yetkazib berish haqi manfiy bo'lishi mumkin emas");
        }
        entity.setDeliveryType(type);
        entity.setDeliveryFee(type == DeliveryType.DELIVERY && fee != null ? fee : BigDecimal.ZERO);
    }

    /** totalSum = originalTotalSum - discountAmount + deliveryFee; debtSum = totalSum - paidSum. */
    private void recalculateTotals(SaleOrder entity) {
        BigDecimal discount = entity.getDiscountAmount() != null ? entity.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = entity.getOriginalTotalSum().subtract(discount).add(entity.deliveryFeeOrZero());
        entity.setTotalSum(total);
        entity.setDebtSum(entity.getSalesOrderStatus() == SalesOrderStatus.CANCELLED
                ? BigDecimal.ZERO
                : total.subtract(paidOrZero(entity)));
    }

    private BigDecimal paidOrZero(SaleOrder entity) {
        return entity.getPaidSum() != null ? entity.getPaidSum() : BigDecimal.ZERO;
    }

    private User getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}