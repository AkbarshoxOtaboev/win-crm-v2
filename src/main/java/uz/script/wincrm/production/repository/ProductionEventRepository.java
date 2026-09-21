package uz.script.wincrm.production.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.production.ProductionEvent;

import java.util.List;

@Repository
public interface ProductionEventRepository extends JpaRepository<ProductionEvent, Long> {
    List<ProductionEvent> findByProductionOrder_IdOrderByOccurredAtAscIdAsc(Long productionOrderId);
}
