package uz.script.wincrm.production.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.production.ProductionOrder;

import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    Optional<ProductionOrder> findBySaleOrder_Id(Long saleOrderId);

    boolean existsBySaleOrder_Id(Long saleOrderId);
}
