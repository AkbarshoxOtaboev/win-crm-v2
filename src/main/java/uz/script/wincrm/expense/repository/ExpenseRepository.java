package uz.script.wincrm.expense.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByCategoryId(Long categoryId, Pageable pageable);

    Optional<Expense> findBySupplierPaymentId(Long supplierPaymentId);

    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    Page<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.expenseDate BETWEEN :startDate AND :endDate AND e.status <> 'DELETED'")
    BigDecimal getTotalAmountByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.category.id = :categoryId AND e.status <> 'DELETED'")
    BigDecimal getTotalAmountByCategory(@Param("categoryId") Long categoryId);

    @Query("""
            select e
            from Expense e
            join fetch e.category c
            where e.expenseDate between :startDate and :endDate
            order by c.name,e.expenseDate
            """)
    List<Expense> findExpenseReportByExpenesCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /** Yetkazib beruvchiga to'lovlarsiz xarajatlar: tovar tannarxi foydada alohida ayiriladi. */
    @Query("""
            select e
            from Expense e
            where e.expenseDate between :startDate and :endDate
              and e.supplierPaymentId is null
            """)
    List<Expense> findOperatingExpenses(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Kassadan xarajatga chiqim: row = paymentTypeId, currency, SUM(amount); [fromDate, toDate).
     * Yetkazib beruvchi to'lovlari kassada SupplierPayment orqali hisoblangani uchun chiqarib tashlanadi.
     */
    @Query("SELECT e.paymentType.id, e.currency, COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.paymentType IS NOT NULL AND e.supplierPaymentId IS NULL " +
            "AND e.expenseDate >= :fromDate AND e.expenseDate < :toDate " +
            "GROUP BY e.paymentType.id, e.currency")
    List<Object[]> sumCashByType(@Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate);
}