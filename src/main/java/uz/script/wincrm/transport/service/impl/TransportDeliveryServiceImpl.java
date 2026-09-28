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
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.transport.TransportDelivery;
import uz.script.wincrm.transport.TransportDriver;
import uz.script.wincrm.transport.TransportWorker;
import uz.script.wincrm.transport.TransportWorkerSalary;
import uz.script.wincrm.transport.dto.DeliveryCrewDTO;
import uz.script.wincrm.transport.enums.DeliveryStatus;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;
import uz.script.wincrm.transport.mapper.TransportMapper;
import uz.script.wincrm.transport.repository.TransportDeliveryRepository;
import uz.script.wincrm.transport.repository.TransportDriverRepository;
import uz.script.wincrm.transport.repository.TransportWorkerRepository;
import uz.script.wincrm.transport.repository.TransportWorkerSalaryRepository;
import uz.script.wincrm.transport.response.DeliveryResponse;
import uz.script.wincrm.transport.response.TransportDashboardResponse;
import uz.script.wincrm.transport.service.TransportDeliveryService;
import uz.script.wincrm.transport.service.TransportSalaryService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransportDeliveryServiceImpl implements TransportDeliveryService {

    private static final Set<DeliveryStatus> ACTIVE_STATUSES = EnumSet.of(
            DeliveryStatus.PENDING, DeliveryStatus.ACCEPTED, DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED);

    private final TransportDeliveryRepository deliveryRepository;
    private final TransportDriverRepository driverRepository;
    private final TransportWorkerRepository workerRepository;
    private final TransportWorkerSalaryRepository salaryRepository;
    private final TransportSalaryService salaryService;
    private final TransportMapper mapper;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "TransportDelivery")
    public void createForSaleOrder(SaleOrder saleOrder) {
        if (deliveryRepository.findBySaleOrder_Id(saleOrder.getId()).isPresent()) {
            throw new BadRequestException("Bu buyurtma allaqachon transport bo'limiga yuborilgan");
        }
        TransportDelivery delivery = TransportDelivery.builder()
                .saleOrder(saleOrder)
                .deliveryStatus(DeliveryStatus.PENDING)
                .address(saleOrder.getClient() != null ? saleOrder.getClient().getAddress() : null)
                .sentAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .build();
        if (saleOrder.getFilial() != null) {
            delivery.setFilial(saleOrder.getFilial());
        }
        deliveryRepository.save(delivery);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public void confirmForSaleOrder(SaleOrder saleOrder) {
        TransportDelivery delivery = deliveryRepository.findBySaleOrder_Id(saleOrder.getId())
                .orElseThrow(() -> new BadRequestException("Bu buyurtma uchun transport yetkazishi topilmadi"));

        DeliveryStatus current = delivery.getDeliveryStatus();
        if (current != DeliveryStatus.IN_TRANSIT && current != DeliveryStatus.ARRIVED) {
            throw new BadRequestException(
                    "Transport hali yo'lga chiqmagan. Yetkazilganini faqat yo'lga chiqqan buyurtma uchun tasdiqlash mumkin.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (delivery.getArrivedAt() == null) {
            delivery.setArrivedAt(now);
        }
        delivery.setDeliveryStatus(DeliveryStatus.CONFIRMED);
        delivery.setConfirmedAt(now);
        delivery.setConfirmedBy(currentUser());
        salaryService.accrueForDelivery(delivery);
        deliveryRepository.save(delivery);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public void cancelForSaleOrder(Long saleOrderId) {
        deliveryRepository.findBySaleOrder_Id(saleOrderId).ifPresent(delivery -> {
            if (delivery.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
                return;
            }
            delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
            delivery.setCancelledAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            salaryService.rejectPendingForDelivery(delivery.getId(), "Sotuv buyurtmasi bekor qilindi");
        });
    }

    @Override
    public List<DeliveryResponse> fetchAll(List<DeliveryStatus> statuses) {
        List<TransportDelivery> deliveries = statuses == null || statuses.isEmpty()
                ? deliveryRepository.findAllByOrderBySentAtDesc()
                : deliveryRepository.findByDeliveryStatusInOrderBySentAtDesc(statuses);
        return deliveries.stream().map(mapper::toResponse).toList();
    }

    @Override
    public DeliveryResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Override
    public DeliveryResponse findBySaleOrderId(Long saleOrderId) {
        return deliveryRepository.findBySaleOrder_Id(saleOrderId).map(mapper::toResponse).orElse(null);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public DeliveryResponse accept(Long id, DeliveryCrewDTO dto) {
        TransportDelivery delivery = getOrThrow(id);
        if (delivery.getDeliveryStatus() != DeliveryStatus.PENDING) {
            throw new BadRequestException("Faqat kutilayotgan buyurtmani qabul qilish mumkin");
        }
        applyCrew(delivery, dto);
        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());
        delivery.setAcceptedBy(currentUser());
        return mapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public DeliveryResponse updateCrew(Long id, DeliveryCrewDTO dto) {
        TransportDelivery delivery = getOrThrow(id);
        if (delivery.getDeliveryStatus() == DeliveryStatus.PENDING
                || !ACTIVE_STATUSES.contains(delivery.getDeliveryStatus())) {
            throw new BadRequestException("Haydovchi va ishchilarni faqat qabul qilingan, hali tasdiqlanmagan buyurtmada o'zgartirish mumkin");
        }
        applyCrew(delivery, dto);
        return mapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public DeliveryResponse depart(Long id) {
        TransportDelivery delivery = getOrThrow(id);
        if (delivery.getDeliveryStatus() != DeliveryStatus.ACCEPTED) {
            throw new BadRequestException("Yo'lga chiqish uchun buyurtma avval qabul qilingan bo'lishi kerak");
        }
        delivery.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setDepartedAt(LocalDateTime.now());
        return mapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "TransportDelivery")
    public DeliveryResponse arrive(Long id) {
        TransportDelivery delivery = getOrThrow(id);
        if (delivery.getDeliveryStatus() != DeliveryStatus.IN_TRANSIT) {
            throw new BadRequestException("Faqat yo'ldagi buyurtmani yetkazildi deb belgilash mumkin");
        }
        delivery.setDeliveryStatus(DeliveryStatus.ARRIVED);
        delivery.setArrivedAt(LocalDateTime.now());
        return mapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    public TransportDashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        List<TransportDelivery> confirmedThisMonth = deliveryRepository.findByDeliveryStatusAndConfirmedAtBetween(
                DeliveryStatus.CONFIRMED, monthStart, monthEnd);
        BigDecimal confirmedSum = confirmedThisMonth.stream()
                .map(d -> d.getSaleOrder().getTotalSum())
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingSalary = sumAmounts(
                salaryRepository.findBySalaryStatusOrderByEarnedAtDescIdDesc(WorkerSalaryStatus.PENDING));
        BigDecimal approvedMonthSalary = sumAmounts(
                salaryRepository.findByPeriodYearAndPeriodMonthOrderByEarnedAtDescIdDesc(
                                today.getYear(), today.getMonthValue()).stream()
                        .filter(s -> s.getSalaryStatus() == WorkerSalaryStatus.APPROVED)
                        .toList());

        return TransportDashboardResponse.builder()
                .pendingCount(deliveryRepository.countByDeliveryStatus(DeliveryStatus.PENDING))
                .acceptedCount(deliveryRepository.countByDeliveryStatus(DeliveryStatus.ACCEPTED))
                .inTransitCount(deliveryRepository.countByDeliveryStatus(DeliveryStatus.IN_TRANSIT))
                .arrivedCount(deliveryRepository.countByDeliveryStatus(DeliveryStatus.ARRIVED))
                .confirmedMonthCount(confirmedThisMonth.size())
                .confirmedMonthSum(confirmedSum)
                .salaryPendingAmount(pendingSalary)
                .salaryApprovedMonthAmount(approvedMonthSalary)
                .workerSalaryPercent(salaryService.currentPercent())
                .activeDrivers(driverRepository.findAll().stream().filter(d -> d.getStatus() == Status.ACTIVE).count())
                .activeWorkers(workerRepository.findAll().stream().filter(w -> w.getStatus() == Status.ACTIVE).count())
                .activeDeliveries(deliveryRepository.findByDeliveryStatusInOrderBySentAtDesc(ACTIVE_STATUSES).stream()
                        .map(mapper::toResponse)
                        .toList())
                .build();
    }

    private void applyCrew(TransportDelivery delivery, DeliveryCrewDTO dto) {
        TransportDriver driver = driverRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Transport driver not found with id: " + dto.getDriverId()));
        if (driver.getStatus() != Status.ACTIVE) {
            throw new BadRequestException("Haydovchi faol emas: " + driver.getFullName());
        }

        Set<Long> workerIds = dto.getWorkerIds() == null ? Set.of() : new LinkedHashSet<>(dto.getWorkerIds());
        Set<TransportWorker> workers = new HashSet<>();
        for (Long workerId : workerIds) {
            TransportWorker worker = workerRepository.findById(workerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transport worker not found with id: " + workerId));
            if (worker.getStatus() != Status.ACTIVE) {
                throw new BadRequestException("Ishchi faol emas: " + worker.getFullName());
            }
            workers.add(worker);
        }

        delivery.setDriver(driver);
        delivery.getWorkers().clear();
        delivery.getWorkers().addAll(workers);
        if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
            delivery.setAddress(dto.getAddress().trim());
        }
        if (dto.getNote() != null) {
            delivery.setNote(dto.getNote().isBlank() ? null : dto.getNote().trim());
        }
    }

    private BigDecimal sumAmounts(List<TransportWorkerSalary> salaries) {
        return salaries.stream()
                .map(TransportWorkerSalary::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransportDelivery getOrThrow(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport delivery not found with id: " + id));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }
}
