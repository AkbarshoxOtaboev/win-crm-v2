package uz.script.wincrm.sale.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItem, Long> {

    List<SaleOrderItem> findAllBySaleOrderId(Long saleOrderId);

    Page<SaleOrderItem> findAllBySaleOrderId(Long saleOrderId, Pageable pageable);

    Page<SaleOrderItem> findByClientId(Long clientId, Pageable pageable);

    Page<SaleOrderItem> findByUserId(Long userId, Pageable pageable);

    Page<SaleOrderItem> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<SaleOrderItem> findByGoodsId(Long goodsId, Pageable pageable);

    List<SaleOrderItem> findByArrivalDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Ombarning konkret mahsuloti uchun jami stockni hisoblaydi
     */
    @Query("SELECT COALESCE(SUM(soi.count), 0) FROM SaleOrderItem soi " +
            "WHERE soi.warehouse.id = :warehouseId AND soi.goods.id = :goodsId AND soi.status <> 'DELETED'")
    BigDecimal getTotalStockByWarehouseAndGoods(
            @Param("warehouseId") Long warehouseId,
            @Param("goodsId") Long goodsId
    );

    @Query("SELECT soi FROM SaleOrderItem soi WHERE soi.saleOrder.id = :saleOrderId AND soi.goods.id = :goodsId")
    Optional<SaleOrderItem> findBySaleOrderIdAndGoodsId(
            @Param("saleOrderId") Long saleOrderId,
            @Param("goodsId") Long goodsId
    );

    /**
     * Goods.type va berilgan sana oralig'i (kunlik/haftalik/oylik) bo'yicha filtrlaydi.
     */
    @Query("SELECT soi FROM SaleOrderItem soi " +
            "WHERE soi.goods.type = :type AND soi.arrivalDate BETWEEN :startDate AND :endDate")
    Page<SaleOrderItem> findByGoodsTypeAndArrivalDateBetween(
            @Param("type") Type type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // ==== DASHBOARD STATISTIKASI ====
    // Sof tushum = SUM(count * priceSelling). Oyna uchun count allaqachon m²
    // (eni * bo'yi * dona / 10000). O'chirilgan buyurtma qatorlari hisobga olinmaydi:
    // buyurtma DELETED bo'lsa ham qator ACTIVE qolishi mumkin.

    /**
     * Mahsulot bo'yicha sotuv qatorlari, buyurtma valyutasi, kursi va kuni bo'yicha alohida -
     * summalar valyutaga o'girilgandan keyin jamlanadi (TOP mahsulotlar, guruhlar).
     * O'chirilgan (DELETED) va bekor qilingan (CANCELLED) buyurtmalar chiqarib tashlanadi.
     * row: goodsId, goodsName, groupId, groupName, currency, exchangeRate, orderDay, SUM(count), SUM(count*price)
     */
    @Query("SELECT g.id, g.name, gg.id, gg.name, so.currency, so.exchangeRate, CAST(so.orderDate AS LocalDate), " +
            "SUM(soi.count), SUM(soi.count * soi.priceSelling) " +
            "FROM SaleOrderItem soi JOIN soi.saleOrder so JOIN soi.goods g LEFT JOIN g.goodsGroup gg " +
            "WHERE soi.arrivalDate BETWEEN :startDate AND :endDate " +
            "AND so.status <> 'DELETED' " +
            "AND so.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED " +
            "GROUP BY g.id, g.name, gg.id, gg.name, so.currency, so.exchangeRate, CAST(so.orderDate AS LocalDate)")
    List<Object[]> findGoodsSalesRows(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}