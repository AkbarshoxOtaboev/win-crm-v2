package uz.script.wincrm.stock.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.exceptions.InsufficientStockException;
import uz.script.wincrm.expense.ImportExpenseKind;
import uz.script.wincrm.expense.repository.ImportExpenseAllocationRepository;
import uz.script.wincrm.expense.repository.ImportExpenseRepository;
import uz.script.wincrm.filial.FilialAccess;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.stock.Stock;
import uz.script.wincrm.stock.StockArrival;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.mapper.StockMapper;
import uz.script.wincrm.stock.repository.StockRepository;
import uz.script.wincrm.stock.response.StockResponse;
import uz.script.wincrm.stock.service.StockHistoryService;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.WarehouseOrder;
import uz.script.wincrm.warehouse.WarehouseOrderItem;
import uz.script.wincrm.warehouse.repository.WarehouseOrderItemRepository;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final GoodsRepository goodsRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMapper stockMapper;
    private final StockHistoryService stockHistoryService;
    private final FilialAccess filialAccess;
    private final WarehouseOrderItemRepository warehouseOrderItemRepository;
    private final ImportExpenseAllocationRepository importAllocationRepository;
    private final ImportExpenseRepository importExpenseRepository;

    @Override
    @Transactional(readOnly = true)
    public StockResponse findById(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + id));
        return stockMapper.toResponse(stock, arrivalOf(stock, arrivalsByKey()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchAll() {
        return present(stockRepository.findAllOrderedByGoodsId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchByWarehouseId(Long warehouseId) {
        return present(stockRepository.findAllByWarehouseId(warehouseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchByGoodsId(Long goodsId) {
        return present(stockRepository.findAllByGoodsId(goodsId));
    }

    private List<StockResponse> present(List<Stock> stocks) {
        Map<String, StockArrival> arrivals = arrivalsByKey();
        return stocks.stream()
                .map(stock -> stockMapper.toResponse(stock, arrivalOf(stock, arrivals)))
                .toList();
    }

    private StockArrival arrivalOf(Stock stock, Map<String, StockArrival> arrivals) {
        if (stock.getGoods() == null || stock.getWarehouse() == null) {
            return null;
        }
        return arrivals.get(stock.getGoods().getId() + ":" + stock.getWarehouse().getId());
    }

    /**
     * Kelgan narx = pozitsiyadagi tannarx + naqd + shu pozitsiyaga taqsimlangan import.
     * Dollar kirimda so‘m qiymati shu hujjat kursiga ko‘paytiriladi.
     */
    private Map<String, StockArrival> arrivalsByKey() {
        List<WarehouseOrderItem> transferred = warehouseOrderItemRepository.findTransferred();
        Map<String, List<WarehouseOrderItem>> grouped = new HashMap<>();
        Set<Long> itemIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        for (WarehouseOrderItem item : transferred) {
            if (item.getGoods() == null || item.getWarehouse() == null || item.getWarehouseOrder() == null) {
                continue;
            }
            if (item.getCount() == null || item.getCount().signum() <= 0) {
                continue;
            }
            String key = item.getGoods().getId() + ":" + item.getWarehouse().getId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
            if (item.getId() != null) {
                itemIds.add(item.getId());
            }
            if (item.getWarehouseOrder().getId() != null) {
                orderIds.add(item.getWarehouseOrder().getId());
            }
        }
        Map<Long, Map<Currency, BigDecimal>> importByItem = importByItem(itemIds);
        Map<String, BigDecimal> importRates = importRates(orderIds);
        Map<String, StockArrival> arrivals = new HashMap<>();
        grouped.forEach((key, items) -> arrivals.put(key, quote(items, importByItem, importRates)));
        return arrivals;
    }

    private Map<Long, Map<Currency, BigDecimal>> importByItem(Set<Long> itemIds) {
        Map<Long, Map<Currency, BigDecimal>> result = new HashMap<>();
        if (itemIds.isEmpty()) {
            return result;
        }
        for (Object[] row : importAllocationRepository.sumByItemAndCurrency(itemIds)) {
            Long itemId = (Long) row[0];
            Currency currency = CurrencyMath.orBase((Currency) row[1]);
            BigDecimal amount = row[2] instanceof BigDecimal d ? d : new BigDecimal(row[2].toString());
            result.computeIfAbsent(itemId, ignored -> new HashMap<>())
                    .merge(currency, amount, BigDecimal::add);
        }
        return result;
    }

    private Map<String, BigDecimal> importRates(Set<Long> orderIds) {
        Map<String, BigDecimal> rates = new HashMap<>();
        if (orderIds.isEmpty()) {
            return rates;
        }
        for (Object[] row : importExpenseRepository.maxRateByOrderGroups(orderIds)) {
            Long orderId = (Long) row[0];
            ImportExpenseKind kind = (ImportExpenseKind) row[1];
            Currency currency = CurrencyMath.orBase((Currency) row[2]);
            BigDecimal rate = row[3] instanceof BigDecimal d ? d : null;
            if (rate != null && rate.signum() > 0) {
                rates.put(orderId + ":" + kind + ":" + currency, rate);
            }
        }
        return rates;
    }

    private StockArrival quote(
            List<WarehouseOrderItem> items,
            Map<Long, Map<Currency, BigDecimal>> importByItem,
            Map<String, BigDecimal> importRates
    ) {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal somValue = BigDecimal.ZERO;
        BigDecimal importSomValue = BigDecimal.ZERO;
        BigDecimal foreignValue = BigDecimal.ZERO;
        Currency foreignCurrency = null;
        boolean hasBase = false;
        boolean mixedCurrency = false;
        WarehouseOrderItem latest = items.get(0);

        for (WarehouseOrderItem item : items) {
            WarehouseOrder order = item.getWarehouseOrder();
            BigDecimal count = item.getCount();
            BigDecimal unit = unitForeign(item);
            qty = qty.add(count);
            somValue = somValue.add(count.multiply(order.arrivalUnitBase(item.getPriceCost(), item.getCashPrice())));
            BigDecimal importSom = importSom(item.getId(), order, importByItem, importRates);
            BigDecimal importForeign = importForeign(item.getId(), order, importByItem, importRates);
            importSomValue = importSomValue.add(importSom);
            somValue = somValue.add(importSom);
            Currency currency = order.getCurrency() != null ? order.getCurrency() : Currency.UZS;
            if (currency.isBase()) {
                hasBase = true;
            } else {
                if (foreignCurrency == null) {
                    foreignCurrency = currency;
                } else if (foreignCurrency != currency) {
                    mixedCurrency = true;
                }
                foreignValue = foreignValue.add(count.multiply(unit)).add(importForeign);
            }
            if (item.getId() != null && (latest.getId() == null || item.getId() > latest.getId())) {
                latest = item;
            }
        }

        BigDecimal unitSom = qty.signum() > 0
                ? somValue.divide(qty, 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal importUnitSom = qty.signum() > 0 && importSomValue.signum() > 0
                ? importSomValue.divide(qty, 2, RoundingMode.HALF_UP)
                : null;
        WarehouseOrder latestOrder = latest.getWarehouseOrder();
        Currency sellingCurrency = latestOrder.getCurrency() != null ? latestOrder.getCurrency() : Currency.UZS;
        BigDecimal selling = latest.getPriceSelling();
        BigDecimal sellingSom = latestOrder.toBase(selling);

        if (mixedCurrency || hasBase || foreignCurrency == null || qty.signum() <= 0) {
            return new StockArrival(unitSom, Currency.UZS.name(), null, null, selling, sellingCurrency.name(), sellingSom, importUnitSom);
        }

        BigDecimal foreignUnit = foreignValue.divide(qty, 2, RoundingMode.HALF_UP);
        BigDecimal rate = latestOrder.getExchangeRate();
        boolean sameRate = true;
        for (WarehouseOrderItem item : items) {
            BigDecimal itemRate = item.getWarehouseOrder().getExchangeRate();
            if (itemRate == null || rate == null || itemRate.compareTo(rate) != 0
                    || item.getWarehouseOrder().getCurrency() != foreignCurrency) {
                sameRate = false;
                break;
            }
        }
        if (!sameRate && foreignUnit.signum() > 0 && unitSom != null) {
            rate = unitSom.divide(foreignUnit, 4, RoundingMode.HALF_UP);
        }
        return new StockArrival(unitSom, foreignCurrency.name(), rate, foreignUnit, selling, sellingCurrency.name(), sellingSom, importUnitSom);
    }

    private BigDecimal unitForeign(WarehouseOrderItem item) {
        BigDecimal price = item.getPriceCost() != null ? item.getPriceCost() : BigDecimal.ZERO;
        BigDecimal cash = item.getCashPrice() != null ? item.getCashPrice() : BigDecimal.ZERO;
        return price.add(cash);
    }

    /** Pozitsiyaga taqsimlangan importning so‘mdagi jami. */
    private BigDecimal importSom(
            Long itemId,
            WarehouseOrder order,
            Map<Long, Map<Currency, BigDecimal>> importByItem,
            Map<String, BigDecimal> importRates
    ) {
        Map<Currency, BigDecimal> byCurrency = itemId == null ? null : importByItem.get(itemId);
        if (byCurrency == null || byCurrency.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal som = BigDecimal.ZERO;
        for (Map.Entry<Currency, BigDecimal> entry : byCurrency.entrySet()) {
            Currency currency = CurrencyMath.orBase(entry.getKey());
            BigDecimal amount = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            if (amount.signum() <= 0) {
                continue;
            }
            if (currency.isBase()) {
                som = som.add(amount);
                continue;
            }
            BigDecimal rate = resolveImportRate(order, currency, importRates);
            if (rate != null && rate.signum() > 0) {
                som = som.add(amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return som;
    }

    /** Pozitsiyaga taqsimlangan importning kirim valyutasidagi jami. */
    private BigDecimal importForeign(
            Long itemId,
            WarehouseOrder order,
            Map<Long, Map<Currency, BigDecimal>> importByItem,
            Map<String, BigDecimal> importRates
    ) {
        Currency orderCurrency = order.getCurrency() != null ? order.getCurrency() : Currency.UZS;
        if (orderCurrency.isBase()) {
            return BigDecimal.ZERO;
        }
        Map<Currency, BigDecimal> byCurrency = itemId == null ? null : importByItem.get(itemId);
        if (byCurrency == null || byCurrency.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal foreign = BigDecimal.ZERO;
        BigDecimal rate = resolveImportRate(order, orderCurrency, importRates);
        for (Map.Entry<Currency, BigDecimal> entry : byCurrency.entrySet()) {
            Currency currency = CurrencyMath.orBase(entry.getKey());
            BigDecimal amount = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            if (amount.signum() <= 0) {
                continue;
            }
            if (currency == orderCurrency) {
                foreign = foreign.add(amount);
            } else if (currency.isBase() && rate != null && rate.signum() > 0) {
                foreign = foreign.add(amount.divide(rate, 2, RoundingMode.HALF_UP));
            }
        }
        return foreign;
    }

    private BigDecimal resolveImportRate(WarehouseOrder order, Currency currency, Map<String, BigDecimal> importRates) {
        Currency cur = CurrencyMath.orBase(currency);
        if (cur.isBase()) {
            return BigDecimal.ONE;
        }
        if (order.getCurrency() == cur && order.getExchangeRate() != null && order.getExchangeRate().signum() > 0) {
            return order.getExchangeRate();
        }
        for (ImportExpenseKind kind : ImportExpenseKind.values()) {
            BigDecimal rate = importRates.get(order.getId() + ":" + kind + ":" + cur);
            if (rate != null && rate.signum() > 0) {
                return rate;
            }
        }
        return order.getExchangeRate();
    }

    @Override
    @Transactional
    public void increaseStock(Long goodsId, Long warehouseId, BigDecimal count, BigDecimal pieceCount,
                              BigDecimal unitCost) {
        BigDecimal pieces = pieceCount != null ? pieceCount : count;
        Stock stock = stockRepository.findByGoodsIdAndWarehouseId(goodsId, warehouseId)
                .map(existing -> {
                    BigDecimal existingPieces = existing.getPieceCount() != null
                            ? existing.getPieceCount()
                            : BigDecimal.ZERO;
                    BigDecimal delta = pieceCount != null ? pieceCount : resolvePieceDelta(existing, count, null);
                    existing.setPriceCost(averageCost(existing, count, unitCost));
                    existing.setCount(existing.getCount().add(count));
                    BigDecimal derived = StockPieces.derive(existing.getGoods(), existing.getCount());
                    existing.setPieceCount(derived != null ? derived : existingPieces.add(delta));
                    return stockRepository.save(existing);
                })
                .orElseGet(() -> {
                    Goods goods = goodsRepository.findById(goodsId)
                            .orElseThrow(() -> new EntityNotFoundException("Goods not found with id: " + goodsId));
                    Warehouse warehouse = warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new EntityNotFoundException("Warehouse not found with id: " + warehouseId));

                    BigDecimal derived = StockPieces.derive(goods, count);
                    Stock newStock = Stock.builder()
                            .goods(goods)
                            .warehouse(warehouse)
                            .count(count)
                            .pieceCount(derived != null ? derived : pieces)
                            .priceCost(unitCost != null ? unitCost : goods.getPriceCost())
                            .status(Status.ACTIVE)
                            .build();
                    filialAccess.attachCurrentFilial(newStock);
                    return stockRepository.save(newStock);
                });

        stockHistoryService.recordIn(goodsId, warehouseId, count, stock.getCount(), "Omborga mahsulot kirim qilindi");
    }

    @Override
    public BigDecimal getAvailableStock(Long goodsId, Long warehouseId) {
        return stockRepository.findByGoodsIdAndWarehouseId(goodsId, warehouseId)
                .map(Stock::getCount)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getUnitCost(Long goodsId, Long warehouseId) {
        if (goodsId == null || warehouseId == null) {
            return null;
        }
        StockArrival arrival = arrivalsByKey().get(goodsId + ":" + warehouseId);
        if (arrival != null && arrival.unitSom() != null && arrival.unitSom().signum() > 0) {
            return arrival.unitSom();
        }
        return stockRepository.findByGoodsIdAndWarehouseId(goodsId, warehouseId)
                .map(Stock::getPriceCost)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> landedUnitCostsByKey() {
        Map<String, BigDecimal> costs = new HashMap<>();
        for (Map.Entry<String, StockArrival> e : arrivalsByKey().entrySet()) {
            StockArrival arrival = e.getValue();
            if (arrival != null && arrival.unitSom() != null && arrival.unitSom().signum() > 0) {
                costs.put(e.getKey(), arrival.unitSom());
            }
        }
        return costs;
    }

    private BigDecimal averageCost(Stock stock, BigDecimal inCount, BigDecimal unitCost) {
        BigDecimal current = stock.getPriceCost();
        if (unitCost == null) {
            return current != null ? current : stock.getGoods().getPriceCost();
        }
        BigDecimal have = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        if (current == null || have.signum() <= 0) {
            return unitCost;
        }
        BigDecimal total = have.add(inCount);
        if (total.signum() <= 0) {
            return unitCost;
        }
        return have.multiply(current).add(inCount.multiply(unitCost))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void decreaseStock(Long goodsId, Long warehouseId, BigDecimal count, BigDecimal pieceCount) {
        Stock stock = stockRepository.findByGoodsIdAndWarehouseId(goodsId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for goodsId: " + goodsId + " and warehouseId: " + warehouseId));

        if (stock.getCount().compareTo(count) < 0) {
            throw new InsufficientStockException("Insufficient stock count for goodsId: " + goodsId);
        }

        BigDecimal piecesDelta = resolvePieceDelta(stock, count, pieceCount);
        BigDecimal currentPieces = stock.getPieceCount() != null ? stock.getPieceCount() : BigDecimal.ZERO;
        if (currentPieces.compareTo(piecesDelta) < 0) {
            piecesDelta = currentPieces;
        }

        stock.setCount(stock.getCount().subtract(count));
        BigDecimal derived = StockPieces.derive(stock.getGoods(), stock.getCount());
        stock.setPieceCount(derived != null ? derived : currentPieces.subtract(piecesDelta));
        stockRepository.save(stock);

        stockHistoryService.recordOut(goodsId, warehouseId, count, stock.getCount(), "Ombordan mahsulot chiqim qilindi");
    }

    private BigDecimal resolvePieceDelta(Stock stock, BigDecimal count, BigDecimal pieceCount) {
        if (pieceCount != null) {
            return pieceCount;
        }
        BigDecimal stockPieces = stock.getPieceCount();
        BigDecimal stockCount = stock.getCount();
        if (stockPieces != null
                && stockCount != null
                && stockCount.compareTo(BigDecimal.ZERO) > 0
                && stockPieces.compareTo(stockCount) != 0) {
            // WINDOW: pieceCount ni count (kv.m) ga proporsional kamaytirish
            return stockPieces.multiply(count)
                    .divide(stockCount, 4, RoundingMode.HALF_UP);
        }
        return count;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + id));
        stock.setStatus(Status.DELETED);
        stockRepository.save(stock);
    }
}
