package uz.script.wincrm.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.expense.ImportExpenseAllocation;
import uz.script.wincrm.expense.ImportExpenseKind;

import java.util.Collection;
import java.util.List;

@Repository
public interface ImportExpenseAllocationRepository extends JpaRepository<ImportExpenseAllocation, Long> {

    List<ImportExpenseAllocation> findByOrderIdAndKindAndCurrency(Long orderId, ImportExpenseKind kind, Currency currency);

    @Query("""
            select a.orderId, a.kind, a.currency, coalesce(sum(a.amount), 0)
            from ImportExpenseAllocation a
            where a.orderId in :orderIds
            group by a.orderId, a.kind, a.currency
            """)
    List<Object[]> sumByOrderGroups(@Param("orderIds") List<Long> orderIds);

    /** Pozitsiya va valyuta bo'yicha taqsimlangan import jami. */
    @Query("""
            select a.itemId, a.currency, coalesce(sum(a.amount), 0)
            from ImportExpenseAllocation a
            where a.itemId in :itemIds
            group by a.itemId, a.currency
            """)
    List<Object[]> sumByItemAndCurrency(@Param("itemIds") Collection<Long> itemIds);
}
