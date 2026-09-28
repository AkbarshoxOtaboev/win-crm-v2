package uz.script.wincrm.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.transport.TransportDelivery;
import uz.script.wincrm.transport.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportDeliveryRepository extends JpaRepository<TransportDelivery, Long> {

    Optional<TransportDelivery> findBySaleOrder_Id(Long saleOrderId);

    List<TransportDelivery> findAllByOrderBySentAtDesc();

    List<TransportDelivery> findByDeliveryStatusInOrderBySentAtDesc(Collection<DeliveryStatus> statuses);

    long countByDeliveryStatus(DeliveryStatus status);

    List<TransportDelivery> findByDeliveryStatusAndConfirmedAtBetween(
            DeliveryStatus status, LocalDateTime from, LocalDateTime to);
}
