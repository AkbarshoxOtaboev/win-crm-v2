package uz.script.wincrm.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.transport.TransportWorkerSalary;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;

import java.util.List;

@Repository
public interface TransportWorkerSalaryRepository extends JpaRepository<TransportWorkerSalary, Long> {

    List<TransportWorkerSalary> findByDelivery_IdOrderByIdAsc(Long deliveryId);

    List<TransportWorkerSalary> findByDelivery_IdAndSalaryStatus(Long deliveryId, WorkerSalaryStatus status);

    List<TransportWorkerSalary> findByPeriodYearAndPeriodMonthOrderByEarnedAtDescIdDesc(Integer year, Integer month);

    List<TransportWorkerSalary> findBySalaryStatusOrderByEarnedAtDescIdDesc(WorkerSalaryStatus status);
}
