package uz.script.wincrm.sale.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

    Page<SaleOrder> findByClientId(Long clientId, Pageable pageable);

    Page<SaleOrder> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<SaleOrder> findByUserId(Long userId, Pageable pageable);

    List<SaleOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /** Foyda hisoboti: bekor qilinmagan buyurtmalar pozitsiyalari, tovari va sotuvchisi bilan. */
    @Query("""
            SELECT DISTINCT so FROM SaleOrder so
            LEFT JOIN FETCH so.user
            LEFT JOIN FETCH so.saleOrderItems soi
            LEFT JOIN FETCH soi.goods g
            LEFT JOIN FETCH g.unitType
            WHERE so.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED
              AND so.orderDate BETWEEN :startDate AND :endDate
            """)
    List<SaleOrder> findForProfitReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Qarzi bor (debtSum > berilgan qiymat) barcha buyurtmalarni topadi.
     * Odatda BigDecimal.ZERO bilan chaqiriladi — qarzi bo'lgan buyurtmalarni olish uchun.
     */
    List<SaleOrder> findByDebtSumGreaterThan(BigDecimal amount);

    /**
     * Bitta mijozning qarzi bor buyurtmalarini topadi.
     */
    List<SaleOrder> findByClient_IdAndDebtSumGreaterThan(Long clientId, BigDecimal amount);

    /** Mijozning bekor qilinmagan, qarzi bor buyurtmalari - eng eskisidan boshlab (FIFO taqsimot uchun). */
    /**
     * Qarzdor mijozlar ro'yxatini (admin panel uchun) ixtiyoriy filtrlar bilan qaytaradi:
     * - startDate/endDate: orderDate bo'yicha sana oralig'i (ikkalasi ham null bo'lsa cheklanmaydi)
     * - userId: buyurtmaga biriktirilgan xodim bo'yicha (null bo'lsa cheklanmaydi)
     *
     * DIQQAT: "CAST(:param AS ...)" bu yerda shart — PostgreSQL JDBC drayveri
     * "(:param IS NULL OR ...)" shablonida parametr faqat IS NULL tekshiruvida
     * ishlatilganda uning tipini avtomatik aniqlay olmaydi va
     * "не удалось определить тип данных параметра $1" xatosini beradi.
     * Aniq CAST bu muammoni butunlay bartaraf etadi.
     */
    @Query("SELECT so FROM SaleOrder so WHERE so.debtSum > 0 " +
            "AND so.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR so.orderDate >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR so.orderDate <= :endDate) " +
            "AND (CAST(:userId AS long) IS NULL OR so.user.id = :userId)")
    List<SaleOrder> findDebtOrders(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("userId") Long userId
    );

    @Query("SELECT so FROM SaleOrder so WHERE so.client.id = :clientId AND so.warehouse.id = :warehouseId")
    Page<SaleOrder> findByClientIdAndWarehouseId(
            @Param("clientId") Long clientId,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    Optional<SaleOrder> findByIdAndWarehouseId(Long id, Long warehouseId);

//    Optional<SaleOrder> findByIdAndStatusNotDeleted(Long id);

    /**
     * Sotuvchilar bo'yicha savdo qatorlari, buyurtma valyutasi, kursi va kuni bo'yicha alohida -
     * summalar valyutaga o'girilgandan keyin jamlanadi.
     * Har bir qator: [0] = userId, [1] = fullName, [2] = currency, [3] = exchangeRate, [4] = kun,
     * [5] = buyurtmalar soni (Long), [6] = summa (BigDecimal).
     * DELETED buyurtmalar SaleOrder'dagi @SQLRestriction("status <> 'DELETED'") orqali avtomatik chiqarib tashlanadi.
     */
    @Query("""
            SELECT u.id, u.fullName, so.currency, so.exchangeRate, CAST(so.orderDate AS LocalDate),
                   COUNT(so), COALESCE(SUM(so.totalSum), 0)
            FROM SaleOrder so JOIN so.user u
            WHERE so.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED
              AND so.orderDate BETWEEN :startDate AND :endDate
            GROUP BY u.id, u.fullName, so.currency, so.exchangeRate, CAST(so.orderDate AS LocalDate)
            """)
    List<Object[]> findSellerSalesRows(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(s.totalSum), 0) FROM SaleOrder s WHERE s.client.id = :clientId " +
            "AND s.currency = :currency " +
            "AND s.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED")
    BigDecimal sumTotalSumByClientIdAndCurrency(@Param("clientId") Long clientId,
                                                @Param("currency") Currency currency);

    @Query("SELECT COALESCE(SUM(s.totalSum), 0) FROM SaleOrder s " +
            "WHERE s.client.id = :clientId AND s.currency = :currency " +
            "AND s.orderDate BETWEEN :fromDateTime AND :toDateTime " +
            "AND s.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED")
    BigDecimal sumTotalSumByClientIdAndCurrencyAndDateRange(
            @Param("clientId") Long clientId,
            @Param("currency") Currency currency,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    @Query("SELECT DISTINCT s.currency FROM SaleOrder s WHERE s.client.id = :clientId " +
            "AND s.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED")
    List<Currency> findCurrenciesByClientId(@Param("clientId") Long clientId);

    /**
     * Berilgan user (sotuvchi) uchun buyurtmalar sonini va umumiy totalSum yig'indisini
     * bitta so'rovda qaytaradi. Natija har doim bitta qatordan iborat bo'ladi:
     * row[0] = buyurtmalar soni (Long), row[1] = umumiy summa (BigDecimal).
     */
    @Query("SELECT COUNT(so), COALESCE(SUM(so.totalSum), 0) FROM SaleOrder so WHERE so.user.id = :userId " +
            "AND so.salesOrderStatus <> uz.script.wincrm.sale.enums.SalesOrderStatus.CANCELLED")
    List<Object[]> countAndSumByUserId(@Param("userId") Long userId);

    /**
     * Rejalashtirilgan yetkazish sanasi (plannedDeliveryDate) o'tib ketgan, lekin buyurtma
     * hali "yetkazilgan/yakunlangan/bekor qilingan" holatlaridan biriga yetmagan
     * (ya'ni kechikkan) buyurtmalarni topadi. "Vaqtida yetkazilyaptimi" nazorati uchun.
     */
    @Query("SELECT so FROM SaleOrder so WHERE so.plannedDeliveryDate IS NOT NULL " +
            "AND so.plannedDeliveryDate < :now " +
            "AND so.salesOrderStatus NOT IN :excludedStatuses")
    List<SaleOrder> findOverdueOrders(
            @Param("now") LocalDateTime now,
            @Param("excludedStatuses") List<SalesOrderStatus> excludedStatuses
    );

    // ---------------------------------------------------------------
    // Telegram bot uchun — lazy proxy'siz, yengil proyeksiyalar
    // ---------------------------------------------------------------

    @Query("SELECT new uz.script.wincrm.telegram.view.SaleOrderView(" +
            "so.id, so.orderDate, so.totalSum, so.paidSum, so.debtSum, so.salesOrderStatus, so.currency) " +
            "FROM SaleOrder so WHERE so.client.id = :clientId ORDER BY so.orderDate DESC")
    List<uz.script.wincrm.telegram.view.SaleOrderView> findOrderViewsByClientId(@Param("clientId") Long clientId);

    @Query("SELECT new uz.script.wincrm.telegram.view.SaleOrderView(" +
            "so.id, so.orderDate, so.totalSum, so.paidSum, so.debtSum, so.salesOrderStatus, so.currency) " +
            "FROM SaleOrder so WHERE so.id = :id AND so.client.id = :clientId")
    Optional<uz.script.wincrm.telegram.view.SaleOrderView> findOrderViewByIdAndClientId(
            @Param("id") Long id, @Param("clientId") Long clientId);
}