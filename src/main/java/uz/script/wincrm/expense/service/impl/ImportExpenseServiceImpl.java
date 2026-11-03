package uz.script.wincrm.expense.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.cash.CashSourceAccess;
import uz.script.wincrm.cash.service.CashAvailabilityService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.expense.ImportExpense;
import uz.script.wincrm.expense.ImportExpenseAllocation;
import uz.script.wincrm.expense.ImportExpenseKind;
import uz.script.wincrm.expense.dto.ImportExpenseDTO;
import uz.script.wincrm.expense.dto.ImportExpenseSplitDTO;
import uz.script.wincrm.expense.dto.ImportExpenseSplitLineDTO;
import uz.script.wincrm.expense.mapper.ImportExpenseMapper;
import uz.script.wincrm.expense.repository.ImportExpenseAllocationRepository;
import uz.script.wincrm.expense.repository.ImportExpenseRepository;
import uz.script.wincrm.expense.response.ImportExpenseItemOption;
import uz.script.wincrm.expense.response.ImportExpenseOrderOption;
import uz.script.wincrm.expense.response.ImportExpenseRecordedResponse;
import uz.script.wincrm.expense.response.ImportExpenseResponse;
import uz.script.wincrm.expense.response.ImportExpenseSplitLineResponse;
import uz.script.wincrm.expense.response.ImportExpenseSplitResponse;
import uz.script.wincrm.expense.service.ImportExpenseService;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.WarehouseOrder;
import uz.script.wincrm.warehouse.WarehouseOrderItem;
import uz.script.wincrm.warehouse.repository.WarehouseOrderItemRepository;
import uz.script.wincrm.warehouse.repository.WarehouseOrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImportExpenseServiceImpl implements ImportExpenseService {

    private final ImportExpenseRepository repository;
    private final ImportExpenseAllocationRepository allocationRepository;
    private final ImportExpenseMapper mapper;
    private final WarehouseOrderRepository orderRepository;
    private final WarehouseOrderItemRepository itemRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final CashAvailabilityService cashAvailabilityService;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "ImportExpense")
    public ImportExpenseResponse create(ImportExpenseDTO dto) {
        WarehouseOrder order = order(dto.getWarehouseOrderId());
        CashSource cashSource = CashSourceAccess.resolve(dto.getCashSource());
        PaymentType type = checkCash(dto.getPaymentTypeId(), dto.getAmount(), null, cashSource);

        ImportExpense entity = ImportExpense.builder()
                .orderId(order.getId())
                .kind(dto.getKind())
                .amount(dto.getAmount())
                .expenseDate(dto.getExpenseDate())
                .description(blankToNull(dto.getDescription()))
                .status(Status.ACTIVE)
                .cashSource(cashSource)
                .build();
        applyOrder(entity, order);
        applyCash(entity, type);
        entity = repository.save(entity);
        log.info("Import expense created. Order {}, kind {}, amount {}", order.getId(), dto.getKind(), dto.getAmount());
        ImportExpenseResponse response = mapper.toResponse(entity);
        response.setAllocatedAmount(BigDecimal.ZERO);
        return response;
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ImportExpense")
    public ImportExpenseResponse update(Long id, ImportExpenseDTO dto) {
        ImportExpense entity = find(id);
        CashSpend previous = CashSpend.of(entity);
        CashSource cashSource = CashSourceAccess.resolve(
                dto.getCashSource() != null ? dto.getCashSource() : previous.cashSource());
        PaymentType type = checkCash(
                dto.getPaymentTypeId() != null ? dto.getPaymentTypeId() : previous.paymentTypeId(),
                dto.getAmount() != null ? dto.getAmount() : entity.getAmount(),
                previous,
                cashSource);

        Long oldOrderId = entity.getOrderId();
        ImportExpenseKind oldKind = entity.getKind();
        Currency oldCurrency = CurrencyMath.orBase(entity.getCurrency());

        if (!dto.getWarehouseOrderId().equals(entity.getOrderId())) {
            WarehouseOrder order = order(dto.getWarehouseOrderId());
            entity.setOrderId(order.getId());
            entity.setItemId(null);
            applyOrder(entity, order);
        }

        entity.setKind(dto.getKind());
        entity.setAmount(dto.getAmount());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setDescription(blankToNull(dto.getDescription()));
        entity.setCashSource(cashSource);
        applyCash(entity, type);
        entity = repository.save(entity);
        clearGroupIfEmpty(oldOrderId, oldKind, oldCurrency);
        return withAllocated(List.of(entity)).get(0);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entity = "ImportExpense")
    public void delete(Long id) {
        ImportExpense entity = find(id);
        Long orderId = entity.getOrderId();
        ImportExpenseKind kind = entity.getKind();
        Currency currency = CurrencyMath.orBase(entity.getCurrency());
        entity.setStatus(Status.DELETED);
        repository.save(entity);
        clearGroupIfEmpty(orderId, kind, currency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportExpenseResponse> search(LocalDate startDate, LocalDate endDate, Long warehouseOrderId, ImportExpenseKind kind) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Sana oralig'ini kiriting");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Tugash sanasi boshlanishdan oldin bo'lishi mumkin emas");
        }
        List<ImportExpense> expenses = repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(startDate, endDate).stream()
                .filter(e -> warehouseOrderId == null || warehouseOrderId.equals(e.getOrderId()))
                .filter(e -> kind == null || kind == e.getKind())
                .toList();
        return withAllocated(expenses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportExpenseRecordedResponse> recorded(Long warehouseOrderItemId, ImportExpenseKind kind, Long excludeId) {
        if (warehouseOrderItemId == null || kind == null) {
            throw new BadRequestException("Pozitsiya va xarajat turi kerak");
        }
        long exclude = excludeId != null ? excludeId : 0L;
        return repository.sumByItemAndKind(warehouseOrderItemId, kind, exclude).stream()
                .map(row -> ImportExpenseRecordedResponse.builder()
                        .currency(CurrencyMath.orBase((Currency) row[0]))
                        .amount(row[1] instanceof BigDecimal d ? d : new BigDecimal(row[1].toString()))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportExpenseOrderOption> orders() {
        return orderRepository.findAllWithSupplier().stream()
                .map(o -> ImportExpenseOrderOption.builder()
                        .id(o.getId())
                        .supplierName(o.getSupplier() != null ? o.getSupplier().getName() : "")
                        .arrivalDate(o.getArrivalDate())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportExpenseItemOption> items(Long warehouseOrderId) {
        order(warehouseOrderId);
        return itemRepository.findOrderedByWarehouseOrderId(warehouseOrderId).stream()
                .map(item -> {
                    boolean sized = item.getWeight() != null && item.getHeight() != null
                            && item.getWeight().signum() > 0 && item.getHeight().signum() > 0;
                    String goods = item.getGoods() != null && item.getGoods().getName() != null
                            ? item.getGoods().getName()
                            : "";
                    return ImportExpenseItemOption.builder()
                            .id(item.getId())
                            .goodsName(goods)
                            .width(item.getWeight())
                            .height(item.getHeight())
                            .pieceCount(sized ? (item.getPieceCount() != null ? item.getPieceCount() : item.getCount()) : null)
                            .count(item.getCount())
                            .build();
                })
                .toList();
    }

    private ImportExpense find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import xarajati topilmadi: " + id));
    }

    private WarehouseOrder order(Long id) {
        if (id == null) {
            throw new BadRequestException("Kirim buyurtmani tanlang");
        }
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kirim buyurtma topilmadi: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public ImportExpenseSplitResponse split(Long id) {
        return toSplit(find(id));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ImportExpenseAllocation")
    public ImportExpenseSplitResponse saveSplit(Long id, ImportExpenseSplitDTO dto) {
        ImportExpense expense = find(id);
        Currency currency = CurrencyMath.orBase(expense.getCurrency());
        BigDecimal paymentTotal = paymentTotal(expense.getOrderId(), expense.getKind(), currency);
        List<WarehouseOrderItem> items = itemRepository.findOrderedByWarehouseOrderId(expense.getOrderId());
        Map<Long, WarehouseOrderItem> byId = new HashMap<>();
        for (WarehouseOrderItem item : items) {
            byId.put(item.getId(), item);
        }
        Map<Long, ImportExpenseAllocation> previous = allocationsByItem(expense.getOrderId(), expense.getKind(), currency);

        List<ImportExpenseAllocation> next = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        List<ImportExpenseSplitLineDTO> lines = dto.getLines() != null ? dto.getLines() : List.of();
        for (ImportExpenseSplitLineDTO line : lines) {
            BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
            if (amount.signum() < 0) {
                throw new BadRequestException("Ulush manfiy bo'lishi mumkin emas");
            }
            if (amount.signum() == 0) {
                continue;
            }
            if (!seen.add(line.getWarehouseOrderItemId())) {
                throw new BadRequestException("Pozitsiya ikki marta yozilgan");
            }
            WarehouseOrderItem item = byId.get(line.getWarehouseOrderItemId());
            ImportExpenseAllocation prev = previous.get(line.getWarehouseOrderItemId());
            if (item == null && prev == null) {
                throw new BadRequestException("Pozitsiya shu kirim buyurtmaga tegishli emas");
            }
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            sum = sum.add(amount);
            ImportExpenseAllocation row = ImportExpenseAllocation.builder()
                    .orderId(expense.getOrderId())
                    .kind(expense.getKind())
                    .currency(currency)
                    .itemId(line.getWarehouseOrderItemId())
                    .amount(amount)
                    .status(Status.ACTIVE)
                    .build();
            if (item != null) {
                fillSnapshot(row, item);
            } else {
                copySnapshot(row, prev);
            }
            if (row.getPositionGoodsName() == null) {
                row.setPositionGoodsName("");
            }
            next.add(row);
        }
        if (sum.compareTo(paymentTotal) > 0) {
            throw new BadRequestException("Taqsimot umumiy to'lovlardan oshmasin");
        }
        clearGroup(expense.getOrderId(), expense.getKind(), currency);
        allocationRepository.saveAll(next);
        return toSplit(expense);
    }

    private void applyOrder(ImportExpense entity, WarehouseOrder order) {
        String supplier = order.getSupplier() != null && order.getSupplier().getName() != null
                ? order.getSupplier().getName()
                : "";
        entity.setSupplierName(supplier);
        entity.setOrderArrivalDate(order.getArrivalDate());
    }

    private void clearGroup(Long orderId, ImportExpenseKind kind, Currency currency) {
        if (orderId == null || kind == null) {
            return;
        }
        for (ImportExpenseAllocation row : allocationRepository.findByOrderIdAndKindAndCurrency(
                orderId, kind, CurrencyMath.orBase(currency))) {
            row.setStatus(Status.DELETED);
        }
    }

    private void clearGroupIfEmpty(Long orderId, ImportExpenseKind kind, Currency currency) {
        if (orderId == null || kind == null) {
            return;
        }
        Currency cur = CurrencyMath.orBase(currency);
        if (repository.countByOrderIdAndKindAndCurrency(orderId, kind, cur) == 0) {
            clearGroup(orderId, kind, cur);
        }
    }

    private BigDecimal paymentTotal(Long orderId, ImportExpenseKind kind, Currency currency) {
        BigDecimal total = repository.sumAmountByGroup(orderId, kind, CurrencyMath.orBase(currency));
        return total != null ? total : BigDecimal.ZERO;
    }

    /** Bir pozitsiya uchun bir necha eski yozuv bo'lsa, summalarni qo'shib snapshotni saqlaydi. */
    private Map<Long, ImportExpenseAllocation> allocationsByItem(Long orderId, ImportExpenseKind kind, Currency currency) {
        Map<Long, ImportExpenseAllocation> byItem = new HashMap<>();
        for (ImportExpenseAllocation row : allocationRepository.findByOrderIdAndKindAndCurrency(
                orderId, kind, CurrencyMath.orBase(currency))) {
            ImportExpenseAllocation existing = byItem.get(row.getItemId());
            if (existing == null) {
                byItem.put(row.getItemId(), ImportExpenseAllocation.builder()
                        .itemId(row.getItemId())
                        .amount(nz(row.getAmount()))
                        .positionGoodsName(row.getPositionGoodsName())
                        .positionWidth(row.getPositionWidth())
                        .positionHeight(row.getPositionHeight())
                        .positionPieces(row.getPositionPieces())
                        .positionQty(row.getPositionQty())
                        .build());
            } else {
                existing.setAmount(nz(existing.getAmount()).add(nz(row.getAmount())));
            }
        }
        return byItem;
    }

    private List<ImportExpenseResponse> withAllocated(List<ImportExpense> expenses) {
        Map<String, BigDecimal> allocated = new HashMap<>();
        Map<String, BigDecimal> payments = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        List<Long> orderIds = expenses.stream().map(ImportExpense::getOrderId).distinct().toList();
        if (!orderIds.isEmpty()) {
            for (Object[] row : allocationRepository.sumByOrderGroups(orderIds)) {
                allocated.put(groupKey((Long) row[0], (ImportExpenseKind) row[1], (Currency) row[2]), (BigDecimal) row[3]);
            }
        }
        for (ImportExpense e : expenses) {
            String key = groupKey(e.getOrderId(), e.getKind(), e.getCurrency());
            payments.merge(key, nz(e.getAmount()), BigDecimal::add);
            counts.merge(key, 1, Integer::sum);
        }
        return expenses.stream().map(e -> {
            String key = groupKey(e.getOrderId(), e.getKind(), e.getCurrency());
            ImportExpenseResponse response = mapper.toResponse(e);
            response.setGroupAmount(payments.getOrDefault(key, BigDecimal.ZERO));
            response.setAllocatedAmount(allocated.getOrDefault(key, BigDecimal.ZERO));
            response.setPaymentCount(counts.getOrDefault(key, 1));
            return response;
        }).toList();
    }

    private ImportExpenseSplitResponse toSplit(ImportExpense expense) {
        Currency currency = CurrencyMath.orBase(expense.getCurrency());
        BigDecimal payments = paymentTotal(expense.getOrderId(), expense.getKind(), currency);
        Map<Long, ImportExpenseAllocation> saved = allocationsByItem(expense.getOrderId(), expense.getKind(), currency);
        BigDecimal allocated = saved.values().stream()
                .map(a -> nz(a.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int paymentCount = (int) repository.countByOrderIdAndKindAndCurrency(
                expense.getOrderId(), expense.getKind(), currency);

        List<WarehouseOrderItem> items = itemRepository.findOrderedByWarehouseOrderId(expense.getOrderId());
        List<ImportExpenseSplitLineResponse> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (WarehouseOrderItem item : items) {
            seen.add(item.getId());
            ImportExpenseAllocation row = saved.get(item.getId());
            boolean sized = item.getWeight() != null && item.getHeight() != null
                    && item.getWeight().signum() > 0 && item.getHeight().signum() > 0;
            String goods = item.getGoods() != null && item.getGoods().getName() != null
                    ? item.getGoods().getName()
                    : "";
            lines.add(ImportExpenseSplitLineResponse.builder()
                    .itemId(item.getId())
                    .goodsName(goods)
                    .width(item.getWeight())
                    .height(item.getHeight())
                    .pieces(sized ? (item.getPieceCount() != null ? item.getPieceCount() : item.getCount()) : null)
                    .qty(item.getCount())
                    .amount(row != null ? row.getAmount() : BigDecimal.ZERO)
                    .build());
        }
        for (ImportExpenseAllocation row : saved.values()) {
            if (seen.contains(row.getItemId())) {
                continue;
            }
            lines.add(ImportExpenseSplitLineResponse.builder()
                    .itemId(row.getItemId())
                    .goodsName(row.getPositionGoodsName())
                    .width(row.getPositionWidth())
                    .height(row.getPositionHeight())
                    .pieces(row.getPositionPieces())
                    .qty(row.getPositionQty())
                    .amount(row.getAmount())
                    .build());
        }
        return ImportExpenseSplitResponse.builder()
                .expenseId(expense.getId())
                .warehouseOrderId(expense.getOrderId())
                .supplierName(expense.getSupplierName())
                .kind(expense.getKind())
                .amount(payments)
                .allocatedAmount(allocated)
                .paymentCount(paymentCount)
                .currency(currency)
                .lines(lines)
                .build();
    }

    private static String groupKey(Long orderId, ImportExpenseKind kind, Currency currency) {
        return orderId + ":" + kind + ":" + CurrencyMath.orBase(currency);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static void fillSnapshot(ImportExpenseAllocation row, WarehouseOrderItem item) {
        boolean sized = item.getWeight() != null && item.getHeight() != null
                && item.getWeight().signum() > 0 && item.getHeight().signum() > 0;
        String goods = item.getGoods() != null && item.getGoods().getName() != null
                ? item.getGoods().getName()
                : "";
        row.setPositionGoodsName(goods);
        row.setPositionWidth(item.getWeight());
        row.setPositionHeight(item.getHeight());
        row.setPositionPieces(sized ? (item.getPieceCount() != null ? item.getPieceCount() : item.getCount()) : null);
        row.setPositionQty(item.getCount());
    }

    private static void copySnapshot(ImportExpenseAllocation row, ImportExpenseAllocation prev) {
        row.setPositionGoodsName(prev.getPositionGoodsName() != null ? prev.getPositionGoodsName() : "");
        row.setPositionWidth(prev.getPositionWidth());
        row.setPositionHeight(prev.getPositionHeight());
        row.setPositionPieces(prev.getPositionPieces());
        row.setPositionQty(prev.getPositionQty());
    }

    private record CashSpend(Long paymentTypeId, Currency currency, BigDecimal amount, CashSource cashSource) {
        static CashSpend of(ImportExpense e) {
            return new CashSpend(
                    e.getPaymentType() != null ? e.getPaymentType().getId() : null,
                    CurrencyMath.orBase(e.getCurrency()),
                    e.getAmount(),
                    e.getCashSource() != null ? e.getCashSource() : CashSource.OPERATIONAL);
        }
    }

    private PaymentType checkCash(Long paymentTypeId, BigDecimal amount, CashSpend previous, CashSource cashSource) {
        if (paymentTypeId == null) {
            throw new BadRequestException("Xarajat qaysi kassadan qilinishini tanlang");
        }
        PaymentType type = paymentTypeRepository.findById(paymentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with id: " + paymentTypeId));
        Currency currency = CurrencyMath.orBase(type.getCurrency());
        BigDecimal returned = BigDecimal.ZERO;
        if (previous != null
                && type.getId().equals(previous.paymentTypeId())
                && currency == previous.currency()
                && cashSource == previous.cashSource()) {
            returned = previous.amount() != null ? previous.amount() : BigDecimal.ZERO;
        }
        cashAvailabilityService.requireEnough(type, amount, returned, "import xarajati", cashSource);
        return type;
    }

    private void applyCash(ImportExpense entity, PaymentType type) {
        Currency currency = CurrencyMath.orBase(type.getCurrency());
        entity.setPaymentType(type);
        entity.setCurrency(currency);
        entity.setExchangeRate(currency.isBase() ? null : rateOrNull(currency, entity.getExpenseDate()));
    }

    private BigDecimal rateOrNull(Currency currency, LocalDate date) {
        try {
            return exchangeRateService.rateOn(currency, date);
        } catch (BadRequestException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
