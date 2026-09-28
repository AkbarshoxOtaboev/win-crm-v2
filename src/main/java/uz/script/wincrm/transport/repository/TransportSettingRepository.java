package uz.script.wincrm.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.transport.TransportSetting;

import java.util.Optional;

@Repository
public interface TransportSettingRepository extends JpaRepository<TransportSetting, Long> {
    Optional<TransportSetting> findFirstByOrderByIdAsc();
}
