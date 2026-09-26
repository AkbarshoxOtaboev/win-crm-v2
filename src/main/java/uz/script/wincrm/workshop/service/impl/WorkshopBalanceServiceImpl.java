package uz.script.wincrm.workshop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.production.ProductionOrder;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;
import uz.script.wincrm.production.enums.ProductionOrderStatus;
import uz.script.wincrm.production.repository.ProductionAssignmentRepository;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.WorkshopBalance;
import uz.script.wincrm.workshop.WorkshopBalanceEntry;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;
import uz.script.wincrm.workshop.repository.WorkshopBalanceEntryRepository;
import uz.script.wincrm.workshop.repository.WorkshopBalanceRepository;
import uz.script.wincrm.workshop.repository.WorkshopRepository;
import uz.script.wincrm.workshop.response.WorkshopDashboardResponse;
import uz.script.wincrm.workshop.service.WorkshopBalanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkshopBalanceServiceImpl implements WorkshopBalanceService {

    private final WorkshopRepository workshopRepository;
    private final WorkshopBalanceRepository balanceRepository;
    private final WorkshopBalanceEntryRepository entryRepository;
    private final ProductionAssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public void creditForAssignment(ProductionAssignment assignment, WorkshopBalanceEventType eventType) {
        if (assignment == null || assignment.getId() == null) {
            return;
        }
        if (entryRepository.existsByAssignment_IdAndEventType(assignment.getId(), eventType)) {
            return;
        }

        Workshop workshop = assignment.getWorkshop();
        ProductionOrder order = assignment.getProductionOrder();
        SaleOrder saleOrder = order != null ? order.getSaleOrder() : null;
        if (workshop == null || saleOrder == null) {
            return;
        }

        BigDecimal feePercent = resolveFeePercent(assignment, workshop);
        if (feePercent.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal orderTotal = saleOrder.getTotalSum() != null ? saleOrder.getTotalSum() : BigDecimal.ZERO;
        BigDecimal amount = orderTotal
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (assignment.getFeePercent() == null) {
            assignment.setFeePercent(feePercent);
            assignmentRepository.save(assignment);
        }

        WorkshopBalance balance = getOrCreateBalance(workshop.getId());
        balance.setTotalEarned(nullSafe(balance.getTotalEarned()).add(amount));
        balance.setBalance(nullSafe(balance.getBalance()).add(amount));
        balance.setLastUpdated(LocalDateTime.now());
        balanceRepository.save(balance);

        WorkshopBalanceEntry entry = WorkshopBalanceEntry.builder()
                .workshop(workshop)
                .assignment(assignment)
                .productionOrder(order)
                .saleOrder(saleOrder)
                .eventType(eventType)
                .orderTotalSum(orderTotal)
                .feePercent(feePercent)
                .amount(amount)
                .occurredAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .build();
        entryRepository.save(entry);

        log.info("Workshop {} credited {} for {} (assignment={})",
                workshop.getId(), amount, eventType, assignment.getId());
    }

    @Override
    @Transactional
    public void setAssignmentFeePercent(Long assignmentId, BigDecimal feePercent) {
        if (feePercent == null || feePercent.compareTo(BigDecimal.ZERO) < 0
                || feePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Foiz 0 dan 100 gacha bo‘lishi kerak");
        }

        ProductionAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + assignmentId));

        if (assignment.getAssignmentStatus() != ProductionAssignmentStatus.DONE) {
            throw new BadRequestException("Faqat bajarilgan ish uchun foiz belgilash mumkin");
        }

        assignment.setFeePercent(feePercent);
        assignmentRepository.save(assignment);

        // Recreate/adjust FINISH credit with new percent
        adjustOrCreateCredit(assignment, WorkshopBalanceEventType.FINISH);
        // If ACCEPT already exists, adjust it too so both events use the same designated %
        if (entryRepository.existsByAssignment_IdAndEventType(
                assignment.getId(), WorkshopBalanceEventType.ACCEPT)) {
            adjustOrCreateCredit(assignment, WorkshopBalanceEventType.ACCEPT);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkshopDashboardResponse dashboard(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workshop not found with id: " + workshopId));

        List<ProductionAssignment> open = assignmentRepository
                .findByWorkshop_IdAndAssignmentStatusInOrderByCreatedAtAsc(
                        workshopId,
                        List.of(ProductionAssignmentStatus.PENDING, ProductionAssignmentStatus.ACTIVE))
                .stream()
                .filter(a -> a.getProductionOrder() == null
                        || a.getProductionOrder().getProductionStatus() != ProductionOrderStatus.CANCELLED)
                .toList();

        List<ProductionAssignment> done = assignmentRepository
                .findByWorkshop_IdAndAssignmentStatusInOrderByFinishedAtDesc(
                        workshopId,
                        List.of(ProductionAssignmentStatus.DONE));

        long queuedCount = 0;
        BigDecimal queuedSum = BigDecimal.ZERO;
        long inProgressCount = 0;
        BigDecimal inProgressSum = BigDecimal.ZERO;

        for (ProductionAssignment a : open) {
            BigDecimal sum = orderSum(a);
            if (a.getAssignmentStatus() == ProductionAssignmentStatus.PENDING) {
                queuedCount++;
                queuedSum = queuedSum.add(sum);
            } else if (a.getAssignmentStatus() == ProductionAssignmentStatus.ACTIVE) {
                inProgressCount++;
                inProgressSum = inProgressSum.add(sum);
            }
        }

        BigDecimal doneSum = BigDecimal.ZERO;
        List<WorkshopDashboardResponse.CompletedWorkItem> completed = new ArrayList<>();
        for (ProductionAssignment a : done) {
            BigDecimal sum = orderSum(a);
            doneSum = doneSum.add(sum);
            BigDecimal fee = a.getFeePercent() != null
                    ? a.getFeePercent()
                    : (workshop.getFeePercent() != null ? workshop.getFeePercent() : BigDecimal.ZERO);
            BigDecimal earned = sum.multiply(fee).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            SaleOrder sale = a.getProductionOrder() != null ? a.getProductionOrder().getSaleOrder() : null;
            completed.add(WorkshopDashboardResponse.CompletedWorkItem.builder()
                    .assignmentId(a.getId())
                    .productionOrderId(a.getProductionOrder() != null ? a.getProductionOrder().getId() : null)
                    .saleOrderId(sale != null ? sale.getId() : null)
                    .clientFullName(sale != null && sale.getClient() != null
                            ? sale.getClient().getFullName()
                            : null)
                    .orderTotalSum(sum)
                    .feePercent(fee)
                    .earnedAmount(earned)
                    .acceptedAt(a.getStartedAt())
                    .submittedAt(a.getFinishedAt())
                    .sequenceNo(a.getSequenceNo())
                    .build());
        }

        WorkshopBalance balance = balanceRepository.findByWorkshop_Id(workshopId).orElse(null);

        return WorkshopDashboardResponse.builder()
                .workshopId(workshop.getId())
                .workshopName(workshop.getName())
                .feePercent(workshop.getFeePercent() != null ? workshop.getFeePercent() : BigDecimal.ZERO)
                .balance(balance != null ? nullSafe(balance.getBalance()) : BigDecimal.ZERO)
                .totalEarned(balance != null ? nullSafe(balance.getTotalEarned()) : BigDecimal.ZERO)
                .queuedCount(queuedCount)
                .queuedSum(queuedSum)
                .inProgressCount(inProgressCount)
                .inProgressSum(inProgressSum)
                .doneCount(done.size())
                .doneSum(doneSum)
                .completedWorks(completed)
                .build();
    }

    private void adjustOrCreateCredit(ProductionAssignment assignment, WorkshopBalanceEventType eventType) {
        Workshop workshop = assignment.getWorkshop();
        ProductionOrder order = assignment.getProductionOrder();
        SaleOrder saleOrder = order.getSaleOrder();
        BigDecimal feePercent = assignment.getFeePercent() != null
                ? assignment.getFeePercent()
                : resolveFeePercent(assignment, workshop);
        BigDecimal orderTotal = saleOrder.getTotalSum() != null ? saleOrder.getTotalSum() : BigDecimal.ZERO;
        BigDecimal newAmount = orderTotal
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        var existingOpt = entryRepository.findByAssignment_IdAndEventType(assignment.getId(), eventType);
        WorkshopBalance balance = getOrCreateBalance(workshop.getId());

        if (existingOpt.isPresent()) {
            WorkshopBalanceEntry entry = existingOpt.get();
            BigDecimal oldAmount = nullSafe(entry.getAmount());
            BigDecimal delta = newAmount.subtract(oldAmount);
            balance.setTotalEarned(nullSafe(balance.getTotalEarned()).add(delta));
            balance.setBalance(nullSafe(balance.getBalance()).add(delta));
            balance.setLastUpdated(LocalDateTime.now());
            balanceRepository.save(balance);

            entry.setFeePercent(feePercent);
            entry.setOrderTotalSum(orderTotal);
            entry.setAmount(newAmount);
            entryRepository.save(entry);
            return;
        }

        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        balance.setTotalEarned(nullSafe(balance.getTotalEarned()).add(newAmount));
        balance.setBalance(nullSafe(balance.getBalance()).add(newAmount));
        balance.setLastUpdated(LocalDateTime.now());
        balanceRepository.save(balance);

        entryRepository.save(WorkshopBalanceEntry.builder()
                .workshop(workshop)
                .assignment(assignment)
                .productionOrder(order)
                .saleOrder(saleOrder)
                .eventType(eventType)
                .orderTotalSum(orderTotal)
                .feePercent(feePercent)
                .amount(newAmount)
                .occurredAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .build());
    }

    private WorkshopBalance getOrCreateBalance(Long workshopId) {
        return balanceRepository.findByWorkshopIdForUpdate(workshopId)
                .orElseGet(() -> {
                    Workshop workshop = workshopRepository.findById(workshopId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Workshop not found with id: " + workshopId));
                    WorkshopBalance created = WorkshopBalance.builder()
                            .workshop(workshop)
                            .totalEarned(BigDecimal.ZERO)
                            .totalPaid(BigDecimal.ZERO)
                            .balance(BigDecimal.ZERO)
                            .lastUpdated(LocalDateTime.now())
                            .status(Status.ACTIVE)
                            .build();
                    return balanceRepository.save(created);
                });
    }

    private BigDecimal resolveFeePercent(ProductionAssignment assignment, Workshop workshop) {
        if (assignment.getFeePercent() != null) {
            return assignment.getFeePercent();
        }
        if (workshop.getFeePercent() != null) {
            return workshop.getFeePercent();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal orderSum(ProductionAssignment a) {
        if (a.getProductionOrder() == null || a.getProductionOrder().getSaleOrder() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = a.getProductionOrder().getSaleOrder().getTotalSum();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
