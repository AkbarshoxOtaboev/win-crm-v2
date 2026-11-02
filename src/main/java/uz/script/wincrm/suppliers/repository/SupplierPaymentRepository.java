package uz.script.wincrm.suppliers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.suppliers.SupplierPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends
        JpaRepository<SupplierPayment, Long>,
        JpaSpecificationExecutor<SupplierPayment> {

    boolean existsByPaymentTypeId(Long paymentTypeId);

    /** Kassadan chiqim: row = paymentTypeId, currency, SUM(paidSumm); [fromDate, toDate). */
    @Query("SELECT sp.paymentType.id, sp.currency, COALESCE(SUM(sp.paidSumm), 0) FROM SupplierPayment sp " +
            "WHERE sp.paymentType IS NOT NULL " +
            "AND COALESCE(sp.cashSource, uz.script.wincrm.cash.CashSource.OPERATIONAL) = :cashSource " +
            "AND sp.paidDate >= :fromDate AND sp.paidDate < :toDate " +
            "GROUP BY sp.paymentType.id, sp.currency")
    List<Object[]> sumCashByType(@Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate,
                                 @Param("cashSource") CashSource cashSource);

    /** Qarz valyutasida yopilgan jami (SUMMA + CASH). */
    @Query("""
            select coalesce(sum(coalesce(sp.appliedAmount, sp.paidSumm)), 0)
            from SupplierPayment sp
            where sp.supplier.id = :supplierId
              and coalesce(sp.debtCurrency, sp.currency) = :debtCurrency
            """)
    BigDecimal sumAppliedBySupplierAndDebtCurrency(@Param("supplierId") Long supplierId,
                                                   @Param("debtCurrency") Currency debtCurrency);

    @Query("""
            select distinct coalesce(sp.debtCurrency, sp.currency)
            from SupplierPayment sp
            where sp.supplier.id = :supplierId
            """)
    List<Currency> findDebtCurrenciesBySupplierId(@Param("supplierId") Long supplierId);
}