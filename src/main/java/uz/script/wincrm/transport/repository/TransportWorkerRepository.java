package uz.script.wincrm.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.transport.TransportWorker;

import java.util.List;

@Repository
public interface TransportWorkerRepository extends JpaRepository<TransportWorker, Long> {
    List<TransportWorker> findAllByOrderByIdAsc();
}
