package uz.script.wincrm.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Page<Payment> findBySaleOrderId(Long saleOrderId, Pageable pageable);

    List<Payment> findBySaleOrderId(Long saleOrderId);

    Page<Payment> findByPaymentTypeId(Long paymentTypeId, Pageable pageable);

    boolean existsByPaymentTypeId(Long paymentTypeId);

    Page<Payment> findByClientId(Long clientId, Pageable pageable);

    List<Payment> findByClientId(Long clientId);

    /**
     * Berilgan sana-vaqt oralig'idagi barcha to'lovlarni, PaymentType bilan birga
     * (N+1 muammosining oldini olish uchun JOIN FETCH) qaytaradi.
     * Dashboard hisobotlarida (PaymentType bo'yicha jamlash, kunlik taqsimot) ishlatiladi.
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.paymentType " +
            "WHERE p.paymentDate BETWEEN :fromDate AND :toDate " +
            "ORDER BY p.paymentDate ASC")
    List<Payment> findAllInRangeWithType(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /** Kassaga kirim: row = paymentTypeId, currency, SUM(paymentAmount); [fromDate, toDate). */
    @Query("SELECT p.paymentType.id, p.currency, COALESCE(SUM(p.paymentAmount), 0) FROM Payment p " +
            "WHERE p.paymentType IS NOT NULL AND p.paymentDate >= :fromDate AND p.paymentDate < :toDate " +
            "GROUP BY p.paymentType.id, p.currency")
    List<Object[]> sumCashByType(@Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate);

    /** Xorijiy valyutadagi qarzni yopgan to'lovlar - kurs farqi hisoboti uchun. */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.saleOrder LEFT JOIN FETCH p.client " +
            "WHERE p.debtCurrency <> uz.script.wincrm.currency.Currency.UZS " +
            "AND p.paymentDate BETWEEN :fromDate AND :toDate " +
            "ORDER BY p.paymentDate ASC, p.id ASC")
    List<Payment> findForeignDebtPaymentsInRange(@Param("fromDate") LocalDateTime fromDate,
                                                 @Param("toDate") LocalDateTime toDate);

    /** Mijozning shu valyutadagi qarzidan yopilgan summa (appliedAmount, eski yozuvlarda paymentAmount). */
    @Query("SELECT COALESCE(SUM(COALESCE(p.appliedAmount, p.paymentAmount)), 0) FROM Payment p " +
            "WHERE p.client.id = :clientId AND p.debtCurrency = :currency")
    BigDecimal sumAppliedByClientIdAndCurrency(@Param("clientId") Long clientId,
                                               @Param("currency") Currency currency);

    @Query("SELECT COALESCE(SUM(COALESCE(p.appliedAmount, p.paymentAmount)), 0) FROM Payment p " +
            "WHERE p.client.id = :clientId AND p.debtCurrency = :currency " +
            "AND p.paymentDate BETWEEN :fromDateTime AND :toDateTime")
    BigDecimal sumAppliedByClientIdAndCurrencyAndDateRange(
            @Param("clientId") Long clientId,
            @Param("currency") Currency currency,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    @Query("SELECT DISTINCT p.debtCurrency FROM Payment p WHERE p.client.id = :clientId")
    List<Currency> findDebtCurrenciesByClientId(@Param("clientId") Long clientId);

    /**
     * Berilgan user (to'lovni qabul qilgan/kiritgan xodim) bo'yicha barcha
     * paymentAmount qiymatlarining yig'indisini qaytaradi.
     */
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.user.id = :userId")
    BigDecimal sumPaymentAmountByUserId(@Param("userId") Long userId);

    // ---------------------------------------------------------------
    // Telegram bot uchun — lazy proxy'siz, yengil proyeksiyalar
    // LEFT JOIN ishlatilgan — paymentType yoki saleOrder null bo'lsa ham
    // yozuv natijadan tushib qolmasligi uchun (ichki JOIN bo'lganda tushib qolardi).
    // ---------------------------------------------------------------

    @Query("SELECT new uz.script.wincrm.telegram.view.PaymentView(" +
            "p.id, p.paymentDate, p.paymentAmount, pt.name, so.id, p.comment, " +
            "p.currency, COALESCE(p.appliedAmount, p.paymentAmount), p.debtCurrency) " +
            "FROM Payment p " +
            "LEFT JOIN p.paymentType pt " +
            "LEFT JOIN p.saleOrder so " +
            "WHERE p.client.id = :clientId " +
            "ORDER BY p.paymentDate DESC")
    List<uz.script.wincrm.telegram.view.PaymentView> findPaymentViewsByClientId(@Param("clientId") Long clientId);

    @Query("SELECT new uz.script.wincrm.telegram.view.PaymentView(" +
            "p.id, p.paymentDate, p.paymentAmount, pt.name, p.saleOrder.id, p.comment, " +
            "p.currency, COALESCE(p.appliedAmount, p.paymentAmount), p.debtCurrency) " +
            "FROM Payment p LEFT JOIN p.paymentType pt " +
            "WHERE p.saleOrder.id = :saleOrderId ORDER BY p.paymentDate ASC")
    List<uz.script.wincrm.telegram.view.PaymentView> findPaymentViewsBySaleOrderId(@Param("saleOrderId") Long saleOrderId);
}