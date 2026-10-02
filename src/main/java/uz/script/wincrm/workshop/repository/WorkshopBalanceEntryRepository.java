package uz.script.wincrm.workshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.workshop.WorkshopBalanceEntry;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopBalanceEntryRepository extends JpaRepository<WorkshopBalanceEntry, Long> {

    Optional<WorkshopBalanceEntry> findByAssignment_IdAndEventType(
            Long assignmentId,
            WorkshopBalanceEventType eventType
    );

    boolean existsByAssignment_IdAndEventType(Long assignmentId, WorkshopBalanceEventType eventType);

    /** [workshopId, count, sum] */
    @Query("select e.workshop.id, count(e), coalesce(sum(e.amount), 0) from WorkshopBalanceEntry e "
            + "where e.occurredAt >= :from and e.occurredAt < :to group by e.workshop.id")
    List<Object[]> aggregateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [workshopId, sum] */
    @Query("select e.workshop.id, coalesce(sum(e.amount), 0) from WorkshopBalanceEntry e group by e.workshop.id")
    List<Object[]> totalsByWorkshop();

    List<WorkshopBalanceEntry> findAllByWorkshop_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(
            Long workshopId, LocalDateTime from, LocalDateTime to);

    List<WorkshopBalanceEntry> findAllByWorkshop_IdOrderByOccurredAtAscIdAsc(Long workshopId);
}
