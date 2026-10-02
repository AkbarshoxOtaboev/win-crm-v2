package uz.script.wincrm.stock.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.stock.StockTransfer;
import uz.script.wincrm.stock.response.StockTransferResponse;

import java.util.Objects;

@Component
public class StockTransferMapper {

    public StockTransferResponse toResponse(StockTransfer transfer) {
        return toResponse(transfer, null);
    }

    public StockTransferResponse toResponse(StockTransfer transfer, Long viewerFilialId) {
        Filial fromFilial = transfer.getFilial();
        Filial toFilial = transfer.getToFilial() != null
                ? transfer.getToFilial()
                : transfer.getToWarehouse() != null ? transfer.getToWarehouse().getFilial() : null;
        Long fromFilialId = fromFilial != null ? fromFilial.getId() : null;
        Long toFilialId = toFilial != null ? toFilial.getId() : null;
        Goods toGoods = transfer.getToGoods() != null ? transfer.getToGoods() : transfer.getGoods();

        return StockTransferResponse.builder()
                .id(transfer.getId())
                .goodsId(transfer.getGoods() != null ? transfer.getGoods().getId() : null)
                .goodsName(transfer.getGoods() != null ? transfer.getGoods().getName() : null)
                .fromWarehouseId(transfer.getFromWarehouse() != null ? transfer.getFromWarehouse().getId() : null)
                .fromWarehouseName(transfer.getFromWarehouse() != null ? transfer.getFromWarehouse().getName() : null)
                .toWarehouseId(transfer.getToWarehouse() != null ? transfer.getToWarehouse().getId() : null)
                .toWarehouseName(transfer.getToWarehouse() != null ? transfer.getToWarehouse().getName() : null)
                .fromFilialId(fromFilialId)
                .fromFilialName(fromFilial != null ? fromFilial.getName() : null)
                .toFilialId(toFilialId)
                .toFilialName(toFilial != null ? toFilial.getName() : null)
                .toGoodsId(toGoods != null ? toGoods.getId() : null)
                .toGoodsName(toGoods != null ? toGoods.getName() : null)
                .direction(direction(fromFilialId, toFilialId, viewerFilialId))
                .count(transfer.getCount())
                .comment(transfer.getComment())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .createdUsername(transfer.getCreatedUsername())
                .build();
    }

    private String direction(Long fromFilialId, Long toFilialId, Long viewerFilialId) {
        if (Objects.equals(fromFilialId, toFilialId)) {
            return "INTERNAL";
        }
        if (viewerFilialId != null && viewerFilialId.equals(toFilialId)) {
            return "INCOMING";
        }
        return "OUTGOING";
    }
}
