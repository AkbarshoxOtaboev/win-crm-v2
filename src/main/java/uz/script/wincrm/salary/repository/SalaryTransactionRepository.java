package uz.script.wincrm.salary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.salary.SalaryTransaction;
import uz.script.wincrm.salary.response.SalarySlipAggregate;

import java.math.BigDecimal;

@Repository
public interface SalaryTransactionRepository extends JpaRepository<SalaryTransaction, Long> {

    Page<SalaryTransaction> findByUserId(Long userId, Pageable pageable);

    Page<SalaryTransaction> findByUserIdAndPeriodYearAndPeriodMonth(
            Long userId, Integer periodYear, Integer periodMonth, Pageable pageable);

    /**
     * Bitta buyurtma bo'yicha ayni paytda "kitobga olingan" sof komissiya:
     *   SUM(COMMISSION) - SUM(COMMISSION_REVERSAL).
     * Commission engine kutilgan qiymat bilan solishtirib delta yozadi (single source of truth).
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN t.entryType = 'COMMISSION' THEN t.amount " +
            "WHEN t.entryType = 'COMMISSION_REVERSAL' THEN -t.amount ELSE 0 END), 0) " +
            "FROM SalaryTransaction t WHERE t.saleOrderId = :saleOrderId")
    BigDecimal netBookedCommissionBySaleOrderId(@Param("saleOrderId") Long saleOrderId);

    /**
     * Bir xodimning bitta davri (yil+oy) uchun yig'ma summalar - oylik slip uchun.
     * Barcha qiymatlar musbat magnitud sifatida qaytadi.
     */
    @Query("SELECT new uz.script.wincrm.salary.response.SalarySlipAggregate(" +
            "COALESCE(SUM(CASE WHEN t.entryType = 'COMMISSION' THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.entryType = 'COMMISSION_REVERSAL' THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.entryType = 'BONUS' THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.entryType = 'DEDUCTION' THEN t.amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.entryType = 'ADVANCE' THEN t.amount ELSE 0 END), 0)) " +
            "FROM SalaryTransaction t " +
            "WHERE t.userId = :userId AND t.periodYear = :periodYear AND t.periodMonth = :periodMonth")
    SalarySlipAggregate aggregateForPeriod(@Param("userId") Long userId,
                                           @Param("periodYear") Integer periodYear,
                                           @Param("periodMonth") Integer periodMonth);
}
