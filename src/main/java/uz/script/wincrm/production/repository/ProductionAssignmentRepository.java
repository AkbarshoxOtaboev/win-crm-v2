package uz.script.wincrm.production.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;

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

    int countByProductionOrder_Id(Long productionOrderId);
}
