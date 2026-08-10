package uz.script.wincrm.sale.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.sale.SaleOrderImage;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleOrderImageRepository extends JpaRepository<SaleOrderImage, Long> {

    List<SaleOrderImage> findBySaleOrderIdOrderByCreatedAtAsc(Long saleOrderId);

    Optional<SaleOrderImage> findByIdAndSaleOrderId(Long id, Long saleOrderId);

    long countBySaleOrderId(Long saleOrderId);
}