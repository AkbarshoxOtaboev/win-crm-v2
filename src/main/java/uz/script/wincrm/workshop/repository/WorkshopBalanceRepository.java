package uz.script.wincrm.workshop.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.workshop.WorkshopBalance;

import java.util.Optional;

@Repository
public interface WorkshopBalanceRepository extends JpaRepository<WorkshopBalance, Long> {

    Optional<WorkshopBalance> findByWorkshop_Id(Long workshopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wb from WorkshopBalance wb where wb.workshop.id = :workshopId")
    Optional<WorkshopBalance> findByWorkshopIdForUpdate(@Param("workshopId") Long workshopId);
}
