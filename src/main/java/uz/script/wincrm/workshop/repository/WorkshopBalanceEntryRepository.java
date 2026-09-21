package uz.script.wincrm.workshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.workshop.WorkshopBalanceEntry;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;

import java.util.Optional;

@Repository
public interface WorkshopBalanceEntryRepository extends JpaRepository<WorkshopBalanceEntry, Long> {

    Optional<WorkshopBalanceEntry> findByAssignment_IdAndEventType(
            Long assignmentId,
            WorkshopBalanceEventType eventType
    );

    boolean existsByAssignment_IdAndEventType(Long assignmentId, WorkshopBalanceEventType eventType);
}
