package uz.script.wincrm.stock.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.stock.Stock;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.response.StockResponse;

import java.math.BigDecimal;

@Component
public class StockMapper {

    public StockResponse toResponse(Stock stock) {
        Goods goods = stock.getGoods();
        BigDecimal count = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        boolean isWindow = goods != null && goods.getType() == Type.WINDOW;
        BigDecimal width = goods != null ? goods.getWidth() : null;
        BigDecimal height = goods != null ? goods.getHeight() : null;

        BigDecimal kvm = isWindow ? count : null;
        BigDecimal pieceCount = StockPieces.derive(goods, count);
        if (pieceCount == null) {
            pieceCount = stock.getPieceCount() != null ? stock.getPieceCount() : count;
        }

        return StockResponse.builder()
                .id(stock.getId())
                .goodsId(goods != null ? goods.getId() : null)
                .goodsName(goods != null ? goods.getName() : null)
                .goodsType(goods != null ? goods.getType() : null)
                .unitTypeName(goods != null && goods.getUnitType() != null
                        ? goods.getUnitType().getName()
                        : null)
                .width(width)
                .height(height)
                .warehouseId(stock.getWarehouse() != null ? stock.getWarehouse().getId() : null)
                .warehouseName(stock.getWarehouse() != null ? stock.getWarehouse().getName() : null)
                .count(count)
                .pieceCount(pieceCount)
                .kvm(kvm)
                .status(stock.getStatus())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .createdUsername(stock.getCreatedUsername())
                .build();
    }
}
