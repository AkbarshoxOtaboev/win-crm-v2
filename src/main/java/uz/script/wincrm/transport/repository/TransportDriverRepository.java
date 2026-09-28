package uz.script.wincrm.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.transport.TransportDriver;

import java.util.List;

@Repository
public interface TransportDriverRepository extends JpaRepository<TransportDriver, Long> {
    List<TransportDriver> findAllByOrderByIdAsc();
}
