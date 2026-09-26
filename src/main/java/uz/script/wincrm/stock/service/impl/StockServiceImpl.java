package uz.script.wincrm.stock.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.exceptions.InsufficientStockException;
import uz.script.wincrm.filial.FilialAccess;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.repository.GoodsRepository;
import uz.script.wincrm.stock.Stock;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.mapper.StockMapper;
import uz.script.wincrm.stock.repository.StockRepository;
import uz.script.wincrm.stock.response.StockResponse;
import uz.script.wincrm.stock.service.StockHistoryService;
import uz.script.wincrm.stock.service.StockService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final GoodsRepository goodsRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMapper stockMapper;
    private final StockHistoryService stockHistoryService;
    private final FilialAccess filialAccess;

    @Override
    @Transactional(readOnly = true)
    public StockResponse findById(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found with id: " + id));
        return stockMapper.toResponse(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchAll() {
        return stockRepository.findAll()
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchByWarehouseId(Long warehouseId) {
        return stockRepository.findAllByWarehouseId(warehouseId)
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> fetchByGoodsId(Long goodsId) {
        return stockRepository.findAllByGoodsId(goodsId)
                .stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void increaseStock(Long goodsId, Long warehouseId, BigDecimal count, BigDecimal pieceCount) {
        BigDecimal pieces = pieceCount != null ? pieceCount : count;
        Stock stock = stockRepository.findByGoodsIdAndWarehouseId(goodsId, warehouseId)
                .map(existing -> {
                    BigDecimal existingPieces = existing.getPieceCount() != null
                            ? existing.getPieceCount()
                            : BigDecimal.ZERO;
                    BigDecimal delta = pieceCount != null ? pieceCount : resolvePieceDelta(existing, count, null);
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
