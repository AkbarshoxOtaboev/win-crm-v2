package uz.script.wincrm.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.warehouse.WarehouseOrderItem;

import java.util.List;

@Repository
public interface WarehouseOrderItemRepository extends JpaRepository<WarehouseOrderItem, Long> {
    List<WarehouseOrderItem> findAllByWarehouseOrderId(Long warehouseOrderId);

    List<WarehouseOrderItem> findAllByWarehouseId(Long warehouseId);

    /** Berilgan mahsulot (Goods) bo'yicha barcha ombor kirim yozuvlarini qaytaradi. */
    List<WarehouseOrderItem> findAllByGoodsId(Long goodsId);

    /** orderId, naqd jami (cashPrice × count), yig'indi ((priceCost + cashPrice) × count). */
    @Query("""
            select i.warehouseOrder.id,
                   coalesce(sum(coalesce(i.cashPrice, 0) * i.count), 0),
                   coalesce(sum((i.priceCost + coalesce(i.cashPrice, 0)) * i.count), 0)
            from WarehouseOrderItem i
            group by i.warehouseOrder.id
            """)
    List<Object[]> sumCashByOrder();
}