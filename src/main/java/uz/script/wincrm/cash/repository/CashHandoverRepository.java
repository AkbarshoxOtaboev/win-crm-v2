package uz.script.wincrm.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.cash.CashHandover;
import uz.script.wincrm.cash.CashHandoverStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface CashHandoverRepository extends JpaRepository<CashHandover, Long> {

    @Query("SELECT h FROM CashHandover h JOIN FETCH h.cashier JOIN FETCH h.paymentType LEFT JOIN FETCH h.reviewer " +
            "WHERE h.handoverDate >= :fromDate AND h.handoverDate <= :toDate " +
            "AND (CAST(:cashierId AS long) IS NULL OR h.cashier.id = :cashierId) " +
            "ORDER BY h.handoverDate DESC, h.id DESC")
    List<CashHandover> findForList(@Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   @Param("cashierId") Long cashierId);

    /** row = paymentTypeId, currency, handoverStatus, SUM(amount) - xodimning shu kungi topshirishlari. */
    @Query("SELECT h.paymentType.id, h.currency, h.handoverStatus, COALESCE(SUM(h.amount), 0) FROM CashHandover h " +
            "WHERE h.cashier.id = :cashierId AND h.handoverDate = :date AND h.handoverStatus IN :statuses " +
            "GROUP BY h.paymentType.id, h.currency, h.handoverStatus")
    List<Object[]> sumForCashierAndDate(@Param("cashierId") Long cashierId,
                                        @Param("date") LocalDate date,
                                        @Param("statuses") Collection<CashHandoverStatus> statuses);

    /** Kassadan rahbarga o'tgan pul: row = paymentTypeId, currency, SUM(amount); [fromDate, toDate). */
    @Query("SELECT h.paymentType.id, h.currency, COALESCE(SUM(h.amount), 0) FROM CashHandover h " +
            "WHERE h.handoverStatus = uz.script.wincrm.cash.CashHandoverStatus.ACCEPTED " +
            "AND h.handoverDate >= :fromDate AND h.handoverDate < :toDate " +
            "GROUP BY h.paymentType.id, h.currency")
    List<Object[]> sumAcceptedByType(@Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);
}
