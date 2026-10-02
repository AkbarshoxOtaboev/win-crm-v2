package uz.script.wincrm.kpi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.kpi.KpiRate;

import java.util.Optional;

@Repository
public interface KpiRateRepository extends JpaRepository<KpiRate, Long> {
    Optional<KpiRate> findByUser_Id(Long userId);
}
