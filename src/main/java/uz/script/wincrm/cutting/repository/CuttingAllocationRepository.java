package uz.script.wincrm.cutting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.script.wincrm.cutting.CuttingAllocation;

import java.util.List;

public interface CuttingAllocationRepository extends JpaRepository<CuttingAllocation, Long> {

    List<CuttingAllocation> findBySaleOrder_Id(Long saleOrderId);

    boolean existsBySaleOrder_Id(Long saleOrderId);
}
