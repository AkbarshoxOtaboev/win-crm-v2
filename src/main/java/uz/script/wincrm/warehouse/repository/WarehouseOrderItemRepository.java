package uz.script.wincrm.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.warehouse.WarehouseOrderItem;

import java.util.List;

@Repository
public interface WarehouseOrderItemRepository extends JpaRepository<WarehouseOrderItem, Long> {
    List<WarehouseOrderItem> findAllByWarehouseOrderId(Long warehouseOrderId);

    @Query("""
            select distinct i from WarehouseOrderItem i
            join fetch i.warehouseOrder o
            join fetch i.goods g
            join fetch i.warehouse w
            where o.orderStatus = uz.script.wincrm.warehouse.enums.WarehouseOrderStatus.TRANSFERRED
              and i.status = uz.script.wincrm.utils.Status.ACTIVE
            """)
    List<WarehouseOrderItem> findTransferred();

    @Query("""
            select i from WarehouseOrderItem i
            where i.warehouseOrder.id = :orderId
            order by i.sortOrder asc nulls last, i.id asc
            """)
    List<WarehouseOrderItem> findOrderedByWarehouseOrderId(@Param("orderId") Long orderId);

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

    /** Ta'minotchi + valyuta bo'yicha aktiv pozitsiyalarning naqd (cash) jami. */
    @Query("""
            select coalesce(sum(coalesce(i.cashPrice, 0) * i.count), 0)
            from WarehouseOrderItem i
            where i.warehouseOrder.supplier.id = :supplierId
              and i.warehouseOrder.currency = :currency
              and i.status = uz.script.wincrm.utils.Status.ACTIVE
            """)
    java.math.BigDecimal sumCashBySupplierAndCurrency(@Param("supplierId") Long supplierId,
                                                      @Param("currency") uz.script.wincrm.currency.Currency currency);
}