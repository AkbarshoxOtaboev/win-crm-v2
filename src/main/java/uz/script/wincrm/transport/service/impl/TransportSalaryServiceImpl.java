package uz.script.wincrm.transport.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.service.SaleOrderBaseConverter;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.transport.TransportDelivery;
import uz.script.wincrm.transport.TransportSetting;
import uz.script.wincrm.transport.TransportWorker;
import uz.script.wincrm.transport.TransportWorkerSalary;
import uz.script.wincrm.transport.dto.SalaryDecisionDTO;
import uz.script.wincrm.transport.dto.TransportSettingDTO;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;
import uz.script.wincrm.transport.mapper.TransportMapper;
import uz.script.wincrm.transport.repository.TransportSettingRepository;
import uz.script.wincrm.transport.repository.TransportWorkerSalaryRepository;
import uz.script.wincrm.transport.response.TransportSettingResponse;
import uz.script.wincrm.transport.response.WorkerSalaryResponse;
import uz.script.wincrm.transport.response.WorkerSalarySummaryResponse;
import uz.script.wincrm.transport.service.TransportSalaryService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransportSalaryServiceImpl implements TransportSalaryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransportWorkerSalaryRepository salaryRepository;
    private final TransportSettingRepository settingRepository;
    private final TransportMapper mapper;
    private final SaleOrderBaseConverter baseConverter;

    @Override
    public void accrueForDelivery(TransportDelivery delivery) {
        SaleOrder order = delivery.getSaleOrder();
        SaleOrderBaseConverter.Conversion fx = baseConverter.convertToday(
                order, order.getTotalSum() != null ? order.getTotalSum() : BigDecimal.ZERO);
        BigDecimal orderTotal = fx.baseAmount();
        BigDecimal percent = currentPercent();
        List<TransportWorker> workers = delivery.getWorkers().stream()
                .sorted(Comparator.comparing(TransportWorker::getId))
                .toList();

        BigDecimal total = orderTotal.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        delivery.setSalaryPercent(percent);
        delivery.setSalaryTotal(workers.isEmpty() ? BigDecimal.ZERO : total);

        if (workers.isEmpty() || total.signum() <= 0) {
            log.info("Delivery {}: no worker salary accrued (workers={}, total={})",
                    delivery.getId(), workers.size(), total);
            return;
        }

        int count = workers.size();
        BigDecimal share = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(share.multiply(BigDecimal.valueOf(count)));
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            BigDecimal amount = i == 0 ? share.add(remainder) : share;
            salaryRepository.save(TransportWorkerSalary.builder()
                    .delivery(delivery)
                    .worker(workers.get(i))
                    .saleOrderId(order.getId())
                    .orderTotalSnapshot(orderTotal)
                    .sourceCurrency(fx.currency())
                    .sourceAmount(fx.sourceAmount())
                    .exchangeRate(fx.rate())
                    .percentSnapshot(percent)
                    .workersCount(count)
                    .amount(amount)
                    .salaryStatus(WorkerSalaryStatus.PENDING)
                    .earnedAt(now)
                    .periodYear(now.getYear())
                    .periodMonth(now.getMonthValue())
                    .status(Status.ACTIVE)
                    .build());
        }
        log.info("Delivery {}: accrued {} to {} worker(s) at {}%", delivery.getId(), total, count, percent);
    }

    @Override
    public void rejectPendingForDelivery(Long deliveryId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();
        for (TransportWorkerSalary salary
                : salaryRepository.findByDelivery_IdAndSalaryStatus(deliveryId, WorkerSalaryStatus.PENDING)) {
            salary.setSalaryStatus(WorkerSalaryStatus.REJECTED);
            salary.setDecidedAt(now);
            salary.setDecidedBy(actor);
            salary.setComment(reason);
            salaryRepository.save(salary);
        }
    }

    @Override
    public List<WorkerSalaryResponse> fetch(Integer year, Integer month, WorkerSalaryStatus status, Long workerId) {
        LocalDate today = LocalDate.now();
        int y = year != null ? year : today.getYear();
        int m = month != null ? month : today.getMonthValue();
        return salaryRepository.findByPeriodYearAndPeriodMonthOrderByEarnedAtDescIdDesc(y, m).stream()
                .filter(s -> status == null || s.getSalaryStatus() == status)
                .filter(s -> workerId == null || Objects.equals(s.getWorker().getId(), workerId))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<WorkerSalarySummaryResponse> summary(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int y = year != null ? year : today.getYear();
        int m = month != null ? month : today.getMonthValue();

        Map<Long, Totals> byWorker = new LinkedHashMap<>();
        for (TransportWorkerSalary s : salaryRepository.findByPeriodYearAndPeriodMonthOrderByEarnedAtDescIdDesc(y, m)) {
            if (s.getSalaryStatus() == WorkerSalaryStatus.REJECTED) {
                continue;
            }
            TransportWorker worker = s.getWorker();
            Totals totals = byWorker.computeIfAbsent(worker.getId(), id -> new Totals(worker.getFullName()));
            totals.deliveries++;
            if (s.getSalaryStatus() == WorkerSalaryStatus.APPROVED) {
                totals.approved = totals.approved.add(s.getAmount());
            } else {
                totals.pending = totals.pending.add(s.getAmount());
            }
        }

        return byWorker.entrySet().stream()
                .map(e -> WorkerSalarySummaryResponse.builder()
                        .workerId(e.getKey())
                        .workerFullName(e.getValue().name)
                        .deliveriesCount(e.getValue().deliveries)
                        .pendingAmount(e.getValue().pending)
                        .approvedAmount(e.getValue().approved)
                        .build())
                .sorted(Comparator.comparing(WorkerSalarySummaryResponse::getWorkerFullName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportWorkerSalary")
    public List<WorkerSalaryResponse> approve(SalaryDecisionDTO dto) {
        return decide(dto, WorkerSalaryStatus.APPROVED);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportWorkerSalary")
    public List<WorkerSalaryResponse> reject(SalaryDecisionDTO dto) {
        return decide(dto, WorkerSalaryStatus.REJECTED);
    }

    @Override
    public BigDecimal currentPercent() {
        return settingRepository.findFirstByOrderByIdAsc()
                .map(TransportSetting::getWorkerSalaryPercent)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public TransportSettingResponse getSetting() {
        return new TransportSettingResponse(currentPercent());
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportSetting")
    public TransportSettingResponse updateSetting(TransportSettingDTO dto) {
        TransportSetting setting = settingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> TransportSetting.builder().status(Status.ACTIVE).build());
        setting.setWorkerSalaryPercent(dto.getWorkerSalaryPercent().setScale(2, RoundingMode.HALF_UP));
        settingRepository.save(setting);
        return new TransportSettingResponse(setting.getWorkerSalaryPercent());
    }

    private List<WorkerSalaryResponse> decide(SalaryDecisionDTO dto, WorkerSalaryStatus target) {
        List<TransportWorkerSalary> salaries = salaryRepository.findAllById(dto.getIds());
        if (salaries.size() != dto.getIds().stream().distinct().count()) {
            throw new ResourceNotFoundException("Oylik yozuvlaridan ba'zilari topilmadi");
        }
        for (TransportWorkerSalary salary : salaries) {
            if (salary.getSalaryStatus() != WorkerSalaryStatus.PENDING) {
                throw new BadRequestException(
                        "Oylik #" + salary.getId() + " allaqachon ko'rib chiqilgan (" + salary.getSalaryStatus() + ")");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();
        String comment = dto.getComment() != null && !dto.getComment().isBlank() ? dto.getComment().trim() : null;
        for (TransportWorkerSalary salary : salaries) {
            salary.setSalaryStatus(target);
            salary.setDecidedAt(now);
            salary.setDecidedBy(actor);
            if (comment != null) {
                salary.setComment(comment);
            }
        }
        return salaryRepository.saveAll(salaries).stream().map(mapper::toResponse).toList();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    private static final class Totals {
        private final String name;
        private long deliveries;
        private BigDecimal pending = BigDecimal.ZERO;
        private BigDecimal approved = BigDecimal.ZERO;

        private Totals(String name) {
            this.name = name;
        }
    }
}
