package uz.script.wincrm.kpi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.kpi.KpiEntry;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface KpiEntryRepository extends JpaRepository<KpiEntry, Long> {

    boolean existsBySaleOrder_IdAndUser_Id(Long saleOrderId, Long userId);

    List<KpiEntry> findAllBySaleOrder_Id(Long saleOrderId);

    List<KpiEntry> findAllByUser_IdAndPeriodYearAndPeriodMonthOrderByEarnedAtAscIdAsc(
            Long userId, Integer periodYear, Integer periodMonth);

    List<KpiEntry> findAllByUser_IdOrderByEarnedAtAscIdAsc(Long userId);

    /** [userId, count, sum] */
    @Query("select e.user.id, count(e), coalesce(sum(e.amount), 0) from KpiEntry e "
            + "where e.periodYear = :year and e.periodMonth = :month group by e.user.id")
    List<Object[]> aggregateForPeriod(@Param("year") Integer year, @Param("month") Integer month);

    /** [userId, sum] */
    @Query("select e.user.id, coalesce(sum(e.amount), 0) from KpiEntry e group by e.user.id")
    List<Object[]> totalsByUser();

    @Query("select coalesce(sum(e.amount), 0) from KpiEntry e "
            + "where e.user.id = :userId and e.periodYear = :year and e.periodMonth = :month")
    BigDecimal sumForUserPeriod(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);
}
