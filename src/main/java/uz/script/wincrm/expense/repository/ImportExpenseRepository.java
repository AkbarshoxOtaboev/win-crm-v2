package uz.script.wincrm.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.ImportExpense;
import uz.script.wincrm.expense.ImportExpenseKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImportExpenseRepository extends JpaRepository<ImportExpense, Long> {

    List<ImportExpense> findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<ImportExpense> findByOrderIdAndKindAndCurrency(Long orderId, ImportExpenseKind kind, Currency currency);

    @Query("""
            select coalesce(sum(e.amount), 0) from ImportExpense e
            where e.orderId = :orderId and e.kind = :kind and e.currency = :currency
            """)
    BigDecimal sumAmountByGroup(@Param("orderId") Long orderId,
                                @Param("kind") ImportExpenseKind kind,
                                @Param("currency") Currency currency);

    long countByOrderIdAndKindAndCurrency(Long orderId, ImportExpenseKind kind, Currency currency);

    /**
     * Shu pozitsiya va tur bo'yicha valyuta kesimida jami.
     * excludeId — tahrirlanayotgan yozuvning o'zi hisobga kirmaydi (0 bo'lsa hammasi).
     */
    @Query("""
            select e.currency, coalesce(sum(e.amount), 0)
            from ImportExpense e
            where e.itemId = :itemId
              and e.kind = :kind
              and e.id <> :excludeId
            group by e.currency
            """)
    List<Object[]> sumByItemAndKind(@Param("itemId") Long itemId,
                                    @Param("kind") ImportExpenseKind kind,
                                    @Param("excludeId") Long excludeId);

    /** Kassadan chiqim: paymentTypeId, currency, SUM(amount); [fromDate, toDate). */
    @Query("""
            SELECT e.paymentType.id, e.currency, COALESCE(SUM(e.amount), 0) FROM ImportExpense e
            WHERE e.paymentType IS NOT NULL
            AND COALESCE(e.cashSource, uz.script.wincrm.cash.CashSource.OPERATIONAL) = :cashSource
            AND e.expenseDate >= :fromDate AND e.expenseDate < :toDate
            GROUP BY e.paymentType.id, e.currency
            """)
    List<Object[]> sumCashByType(@Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 @Param("cashSource") CashSource cashSource);
}
