package uz.script.wincrm.production.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionAssignmentRepository extends JpaRepository<ProductionAssignment, Long> {
    List<ProductionAssignment> findByProductionOrder_IdOrderBySequenceNoAsc(Long productionOrderId);

    Optional<ProductionAssignment> findFirstByProductionOrder_IdAndAssignmentStatusInOrderBySequenceNoDesc(
            Long productionOrderId,
            List<ProductionAssignmentStatus> statuses
    );

    List<ProductionAssignment> findByWorkshop_IdAndAssignmentStatusInOrderByCreatedAtAsc(
            Long workshopId,
            List<ProductionAssignmentStatus> statuses
    );

    List<ProductionAssignment> findByWorkshop_IdAndAssignmentStatusInOrderByFinishedAtDesc(
            Long workshopId,
            List<ProductionAssignmentStatus> statuses
    );

    int countByProductionOrder_Id(Long productionOrderId);

    /** Sexda [from, to) oralig'ida yakunlangan topshiriqlar. */
    @Query("SELECT a FROM ProductionAssignment a WHERE a.workshop.id = :workshopId " +
            "AND a.assignmentStatus = uz.script.wincrm.production.enums.ProductionAssignmentStatus.DONE " +
            "AND a.finishedAt >= :from AND a.finishedAt < :to ORDER BY a.finishedAt DESC")
    List<ProductionAssignment> findDoneInRange(@Param("workshopId") Long workshopId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    /** Sanalar kiritilmasa chegara qo'yilmaydi; toDate kun oxirigacha kiradi. */
    default List<ProductionAssignment> findDoneBetween(Long workshopId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : LocalDateTime.of(3000, 1, 1, 0, 0);
        return findDoneInRange(workshopId, from, to);
    }
}
