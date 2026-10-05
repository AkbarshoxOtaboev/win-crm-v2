package uz.script.wincrm.suppliers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.script.wincrm.suppliers.SupplierPayment;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends
        JpaRepository<SupplierPayment, Long>,
        JpaSpecificationExecutor<SupplierPayment> {

    boolean existsByPaymentTypeId(Long paymentTypeId);

    /** Kassadan chiqim: row = paymentTypeId, currency, SUM(paidSumm); [fromDate, toDate). */
    @Query("SELECT sp.paymentType.id, sp.currency, COALESCE(SUM(sp.paidSumm), 0) FROM SupplierPayment sp " +
            "WHERE sp.paymentType IS NOT NULL AND sp.paidDate >= :fromDate AND sp.paidDate < :toDate " +
            "GROUP BY sp.paymentType.id, sp.currency")
    List<Object[]> sumCashByType(@Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate);

    /** Xodim kiritgan chiqimlar: row = paymentTypeId, currency, SUM(paidSumm); [fromDate, toDate). */
    @Query("SELECT sp.paymentType.id, sp.currency, COALESCE(SUM(sp.paidSumm), 0) FROM SupplierPayment sp " +
            "WHERE sp.paymentType IS NOT NULL AND sp.createdUserId = :userId " +
            "AND sp.paidDate >= :fromDate AND sp.paidDate < :toDate " +
            "GROUP BY sp.paymentType.id, sp.currency")
    List<Object[]> sumCashByTypeForCreator(@Param("userId") Long userId,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate);
}