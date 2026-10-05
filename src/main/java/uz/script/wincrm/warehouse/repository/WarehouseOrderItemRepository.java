package uz.script.wincrm.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.warehouse.WarehouseOrderItem;

import java.util.List;

@Repository
public interface WarehouseOrderItemRepository extends JpaRepository<WarehouseOrderItem, Long> {
    List<WarehouseOrderItem> findAllByWarehouseOrderId(Long warehouseOrderId);

    boolean existsByWarehouseOrderIdAndGoodsId(Long warehouseOrderId, Long goodsId);

    boolean existsByWarehouseOrderIdAndGoodsIdAndIdNot(Long warehouseOrderId, Long goodsId, Long id);
    List<WarehouseOrderItem> findAllByWarehouseId(Long warehouseId);

    /** Berilgan mahsulot (Goods) bo'yicha barcha ombor kirim yozuvlarini qaytaradi. */
    List<WarehouseOrderItem> findAllByGoodsId(Long goodsId);
}