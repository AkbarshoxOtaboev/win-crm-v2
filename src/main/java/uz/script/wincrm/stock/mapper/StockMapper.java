package uz.script.wincrm.stock.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.stock.Stock;
import uz.script.wincrm.stock.StockArrival;
import uz.script.wincrm.stock.StockPieces;
import uz.script.wincrm.stock.response.StockResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StockMapper {

    public StockResponse toResponse(Stock stock) {
        return toResponse(stock, null);
    }

    public StockResponse toResponse(Stock stock, StockArrival arrival) {
        Goods goods = stock.getGoods();
        BigDecimal count = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        boolean isWindow = goods != null && goods.getType() == Type.WINDOW;
        BigDecimal width = goods != null ? goods.getWidth() : null;
        BigDecimal height = goods != null ? goods.getHeight() : null;
        BigDecimal thickness = goods != null ? goods.getThickness() : null;
        BigDecimal weightPerSqm = goods != null ? goods.getWeightPerSqm() : null;

        BigDecimal kvm = isWindow ? count : null;
        BigDecimal pieceCount = StockPieces.derive(goods, count);
        if (pieceCount == null) {
            pieceCount = stock.getPieceCount() != null ? stock.getPieceCount() : count;
        }
        BigDecimal weight = null;
        if (isWindow && kvm != null && weightPerSqm != null) {
            weight = kvm.multiply(weightPerSqm).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal priceCost = arrival != null && arrival.unitSom() != null
                ? arrival.unitSom()
                : stock.getPriceCost();
        if (priceCost == null && goods != null) {
            priceCost = goods.getPriceCost();
        }
        BigDecimal totalCost = priceCost != null
                ? count.multiply(priceCost).setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal selling = arrival != null ? arrival.selling() : goods != null ? goods.getPriceSelling() : null;
        String sellingCurrency = arrival != null && arrival.sellingCurrency() != null
                ? arrival.sellingCurrency()
                : goods != null && goods.getPriceCurrency() != null ? goods.getPriceCurrency().name() : null;

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
                .thickness(thickness)
                .weightPerSqm(weightPerSqm)
                .weight(weight)
                .warehouseId(stock.getWarehouse() != null ? stock.getWarehouse().getId() : null)
                .warehouseName(stock.getWarehouse() != null ? stock.getWarehouse().getName() : null)
                .count(count)
                .pieceCount(pieceCount)
                .kvm(kvm)
                .priceCost(priceCost)
                .totalCost(totalCost)
                .currency(arrival != null ? arrival.currency() : null)
                .exchangeRate(arrival != null ? arrival.exchangeRate() : null)
                .foreignUnitCost(arrival != null ? arrival.foreignUnit() : null)
                .priceSelling(selling)
                .sellingCurrency(sellingCurrency)
                .sellingBase(arrival != null ? arrival.sellingSom() : selling)
                .status(stock.getStatus())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .createdUsername(stock.getCreatedUsername())
                .build();
    }
}
