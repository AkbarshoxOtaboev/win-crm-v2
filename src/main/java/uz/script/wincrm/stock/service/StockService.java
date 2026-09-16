package uz.script.wincrm.stock.service;

import uz.script.wincrm.stock.response.StockResponse;

import java.math.BigDecimal;
import java.util.List;

public interface StockService {

    StockResponse findById(Long id);

    List<StockResponse> fetchAll();

    List<StockResponse> fetchByWarehouseId(Long warehouseId);

    List<StockResponse> fetchByGoodsId(Long goodsId);

    /**
     * Omborga mahsulot kirim. count — asosiy ombor miqdori (WINDOW: kv.m),
     * pieceCount — dona (WINDOW uchun alohida; null bo'lsa count olinadi).
     */
    void increaseStock(Long goodsId, Long warehouseId, BigDecimal count, BigDecimal pieceCount);

    default void increaseStock(Long goodsId, Long warehouseId, BigDecimal count) {
        increaseStock(goodsId, warehouseId, count, count);
    }

    BigDecimal getAvailableStock(Long goodsId, Long warehouseId);

    void decreaseStock(Long goodsId, Long warehouseId, BigDecimal count, BigDecimal pieceCount);

    default void decreaseStock(Long goodsId, Long warehouseId, BigDecimal count) {
        decreaseStock(goodsId, warehouseId, count, null);
    }

    void delete(Long id);
}
