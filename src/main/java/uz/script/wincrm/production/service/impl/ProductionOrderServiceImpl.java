package uz.script.wincrm.production.service.impl;

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
import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.production.ProductionEvent;
import uz.script.wincrm.production.ProductionOrder;
import uz.script.wincrm.production.dto.CompleteProductionDTO;
import uz.script.wincrm.production.dto.RedirectProductionDTO;
import uz.script.wincrm.production.dto.SendToProductionDTO;
import uz.script.wincrm.production.enums.ProductionAssignmentStatus;
import uz.script.wincrm.production.enums.ProductionEventType;
import uz.script.wincrm.production.enums.ProductionOrderStatus;
import uz.script.wincrm.production.repository.ProductionAssignmentRepository;
import uz.script.wincrm.production.repository.ProductionEventRepository;
import uz.script.wincrm.production.repository.ProductionOrderRepository;
import uz.script.wincrm.production.response.ProductionEventResponse;
import uz.script.wincrm.production.response.ProductionOrderResponse;
import uz.script.wincrm.production.service.ProductionOrderService;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.dto.SaleOrderHistoryDTO;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.service.SaleOrderHistoryService;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;
import uz.script.wincrm.workshop.repository.WorkshopRepository;
import uz.script.wincrm.workshop.service.WorkshopBalanceService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductionOrderServiceImpl implements ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionAssignmentRepository assignmentRepository;
    private final ProductionEventRepository eventRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final WorkshopRepository workshopRepository;
    private final SaleOrderHistoryService saleOrderHistoryService;
    private final WorkshopBalanceService workshopBalanceService;

    @Override
    @Auditable(action = AuditAction.CREATE, entity = "ProductionOrder")
    public ProductionOrderResponse sendToProduction(SendToProductionDTO dto) {
        SaleOrder saleOrder = saleOrderRepository.findById(dto.getSaleOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale order not found with id: " + dto.getSaleOrderId()));

        if (saleOrder.getSalesOrderStatus() != SalesOrderStatus.CONFIRMED
                && saleOrder.getSalesOrderStatus() != SalesOrderStatus.NEW) {
            throw new BadRequestException(
                    "Faqat NEW yoki CONFIRMED buyurtmani ishlab chiqarishga yuborish mumkin. Hozirgi: "
                            + saleOrder.getSalesOrderStatus());
        }

        if (productionOrderRepository.existsBySaleOrder_Id(saleOrder.getId())) {
            throw new BadRequestException("Bu buyurtma allaqachon ishlab chiqarishga yuborilgan");
        }

        Workshop workshop = getActiveWorkshop(dto.getWorkshopId());
        User actor = currentUser();
        LocalDateTime now = LocalDateTime.now();

        if (saleOrder.getSalesOrderStatus() == SalesOrderStatus.NEW) {
            transitionSaleOrder(saleOrder, SalesOrderStatus.CONFIRMED);
        }
        transitionSaleOrder(saleOrder, SalesOrderStatus.PROCESSING);

        ProductionOrder order = ProductionOrder.builder()
                .saleOrder(saleOrder)
                .productionStatus(ProductionOrderStatus.QUEUED)
                .currentWorkshop(workshop)
                .note(dto.getNote())
                .status(Status.ACTIVE)
                .build();
        order = productionOrderRepository.save(order);

        ProductionAssignment assignment = ProductionAssignment.builder()
                .productionOrder(order)
                .workshop(workshop)
                .fromWorkshop(null)
                .assignmentStatus(ProductionAssignmentStatus.PENDING)
                .sequenceNo(1)
                .note(dto.getNote())
                .status(Status.ACTIVE)
                .build();
        assignmentRepository.save(assignment);

        recordEvent(order, ProductionEventType.ASSIGNED, null, workshop, actor, now, dto.getNote());

        return toResponse(order);
    }

    @Override
    public ProductionOrderResponse findById(Long id) {
        return toResponse(getOrder(id));
    }

    @Override
    public List<ProductionOrderResponse> fetchAll() {
        return productionOrderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ProductionOrderResponse> board(Long workshopId) {
        getActiveWorkshop(workshopId);
        return assignmentRepository
                .findByWorkshop_IdAndAssignmentStatusInOrderByCreatedAtAsc(
                        workshopId,
                        List.of(ProductionAssignmentStatus.PENDING, ProductionAssignmentStatus.ACTIVE))
                .stream()
                .filter(a -> a.getProductionOrder().getProductionStatus() != ProductionOrderStatus.CANCELLED)
                .map(a -> toResponse(a.getProductionOrder()))
                .toList();
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ProductionOrder")
    public void cancelForSaleOrder(Long saleOrderId) {
        productionOrderRepository.findBySaleOrder_Id(saleOrderId).ifPresent(order -> {
            if (order.getProductionStatus() == ProductionOrderStatus.DONE
                    || order.getProductionStatus() == ProductionOrderStatus.CANCELLED) {
                return;
            }
            order.setProductionStatus(ProductionOrderStatus.CANCELLED);
            productionOrderRepository.save(order);
            recordEvent(order, ProductionEventType.CANCELLED, null, order.getCurrentWorkshop(),
                    currentUser(), LocalDateTime.now(), "Savdo buyurtmasi bekor qilindi");
        });
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ProductionOrder")
    public ProductionOrderResponse start(Long id) {
        ProductionOrder order = getOrder(id);
        if (order.getProductionStatus() == ProductionOrderStatus.DONE
                || order.getProductionStatus() == ProductionOrderStatus.CANCELLED) {
            throw new BadRequestException("Yakunlangan buyurtmani boshlab bo‘lmaydi");
        }

        ProductionAssignment current = getOpenAssignment(order.getId());
        if (current.getAssignmentStatus() == ProductionAssignmentStatus.ACTIVE) {
            return toResponse(order);
        }
        if (current.getAssignmentStatus() != ProductionAssignmentStatus.PENDING) {
            throw new BadRequestException("Joriy assignmentni boshlab bo‘lmaydi");
        }

        LocalDateTime now = LocalDateTime.now();
        current.setAssignmentStatus(ProductionAssignmentStatus.ACTIVE);
        current.setStartedAt(now);
        assignmentRepository.save(current);

        if (order.getProductionStatus() == ProductionOrderStatus.QUEUED) {
            order.setProductionStatus(ProductionOrderStatus.IN_PROGRESS);
            order.setStartedAt(now);
        }
        order.setCurrentWorkshop(current.getWorkshop());
        productionOrderRepository.save(order);

        recordEvent(order, ProductionEventType.STARTED, null, current.getWorkshop(),
                currentUser(), now, null);

        return toResponse(order);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ProductionOrder")
    public ProductionOrderResponse redirect(Long id, RedirectProductionDTO dto) {
        ProductionOrder order = getOrder(id);
        if (order.getProductionStatus() == ProductionOrderStatus.DONE
                || order.getProductionStatus() == ProductionOrderStatus.CANCELLED) {
            throw new BadRequestException("Yakunlangan buyurtmani yo‘naltirib bo‘lmaydi");
        }

        ProductionAssignment current = getOpenAssignment(order.getId());
        Workshop next = getActiveWorkshop(dto.getNextWorkshopId());
        if (current.getWorkshop().getId().equals(next.getId())) {
            throw new BadRequestException("Keyingi sex joriydan farq qilishi kerak");
        }

        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();

        if (current.getAssignmentStatus() == ProductionAssignmentStatus.PENDING) {
            current.setStartedAt(now);
        }
        current.setAssignmentStatus(ProductionAssignmentStatus.DONE);
        current.setFinishedAt(now);
        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            current.setNote(dto.getNote());
        }
        assignmentRepository.save(current);

        recordEvent(order, ProductionEventType.FINISHED, null, current.getWorkshop(),
                actor, now, dto.getNote());

        workshopBalanceService.creditForAssignment(current, WorkshopBalanceEventType.FINISH);

        int nextSeq = assignmentRepository.countByProductionOrder_Id(order.getId()) + 1;
        ProductionAssignment nextAssignment = ProductionAssignment.builder()
                .productionOrder(order)
                .workshop(next)
                .fromWorkshop(current.getWorkshop())
                .assignmentStatus(ProductionAssignmentStatus.PENDING)
                .sequenceNo(nextSeq)
                .status(Status.ACTIVE)
                .build();
        assignmentRepository.save(nextAssignment);

        order.setCurrentWorkshop(next);
        if (order.getProductionStatus() == ProductionOrderStatus.QUEUED) {
            order.setProductionStatus(ProductionOrderStatus.IN_PROGRESS);
            if (order.getStartedAt() == null) {
                order.setStartedAt(now);
            }
        }
        productionOrderRepository.save(order);

        recordEvent(order, ProductionEventType.REDIRECTED, current.getWorkshop(), next,
                actor, now, dto.getNote());
        recordEvent(order, ProductionEventType.ASSIGNED, current.getWorkshop(), next,
                actor, now, null);

        return toResponse(order);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entity = "ProductionOrder")
    public ProductionOrderResponse complete(Long id, CompleteProductionDTO dto) {
        ProductionOrder order = getOrder(id);
        if (order.getProductionStatus() == ProductionOrderStatus.DONE) {
            return toResponse(order);
        }
        if (order.getProductionStatus() == ProductionOrderStatus.CANCELLED) {
            throw new BadRequestException("Bekor qilingan buyurtmani yakunlab bo‘lmaydi");
        }

        ProductionAssignment current = getOpenAssignment(order.getId());
        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();
        String note = dto != null ? dto.getNote() : null;

        if (current.getAssignmentStatus() == ProductionAssignmentStatus.PENDING) {
            current.setStartedAt(now);
        }
        current.setAssignmentStatus(ProductionAssignmentStatus.DONE);
        current.setFinishedAt(now);
        if (note != null && !note.isBlank()) {
            current.setNote(note);
        }
        assignmentRepository.save(current);

        recordEvent(order, ProductionEventType.FINISHED, null, current.getWorkshop(),
                actor, now, note);

        workshopBalanceService.creditForAssignment(current, WorkshopBalanceEventType.FINISH);

        order.setProductionStatus(ProductionOrderStatus.DONE);
        order.setDoneAt(now);
        order.setCurrentWorkshop(current.getWorkshop());
        productionOrderRepository.save(order);

        recordEvent(order, ProductionEventType.COMPLETED, null, current.getWorkshop(),
                actor, now, note);

        SaleOrder saleOrder = order.getSaleOrder();
        if (saleOrder.getSalesOrderStatus() == SalesOrderStatus.PROCESSING) {
            transitionSaleOrder(saleOrder, SalesOrderStatus.READY);
        }

        return toResponse(order);
    }

    @Override
    public List<ProductionEventResponse> timeline(Long id) {
        getOrder(id);
        return eventRepository.findByProductionOrder_IdOrderByOccurredAtAscIdAsc(id).stream()
                .map(this::toEventResponse)
                .toList();
    }

    private ProductionOrder getOrder(Long id) {
        return productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production order not found with id: " + id));
    }

    private Workshop getActiveWorkshop(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workshop not found with id: " + workshopId));
        if (workshop.getStatus() != Status.ACTIVE) {
            throw new BadRequestException("Sex faol emas: " + workshop.getName());
        }
        return workshop;
    }

    private ProductionAssignment getOpenAssignment(Long productionOrderId) {
        return assignmentRepository
                .findFirstByProductionOrder_IdAndAssignmentStatusInOrderBySequenceNoDesc(
                        productionOrderId,
                        List.of(ProductionAssignmentStatus.PENDING, ProductionAssignmentStatus.ACTIVE))
                .orElseThrow(() -> new BadRequestException(
                        "Ochiq sex assignment topilmadi"));
    }

    private void transitionSaleOrder(SaleOrder saleOrder, SalesOrderStatus target) {
        SalesOrderStatus current = saleOrder.getSalesOrderStatus();
        if (!current.canTransitionTo(target)) {
            throw new BadRequestException(
                    "Cannot change order status from " + current + " to " + target);
        }
        saleOrder.setSalesOrderStatus(target);
        saleOrderRepository.save(saleOrder);
        saleOrderHistoryService.recordHistory(
                SaleOrderHistoryDTO.builder()
                        .saleOrderId(saleOrder.getId())
                        .fromStatus(current)
                        .toStatus(target)
                        .build()
        );
    }

    private void recordEvent(
            ProductionOrder order,
            ProductionEventType type,
            Workshop from,
            Workshop to,
            User actor,
            LocalDateTime at,
            String comment
    ) {
        ProductionEvent event = ProductionEvent.builder()
                .productionOrder(order)
                .eventType(type)
                .fromWorkshop(from)
                .toWorkshop(to)
                .actor(actor)
                .occurredAt(at)
                .comment(comment)
                .status(Status.ACTIVE)
                .build();
        eventRepository.save(event);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    private ProductionOrderResponse toResponse(ProductionOrder order) {
        SaleOrder saleOrder = order.getSaleOrder();
        String clientName = null;
        if (saleOrder != null && saleOrder.getClient() != null) {
            clientName = saleOrder.getClient().getFullName();
        }

        ProductionAssignment current = assignmentRepository
                .findFirstByProductionOrder_IdAndAssignmentStatusInOrderBySequenceNoDesc(
                        order.getId(),
                        List.of(ProductionAssignmentStatus.PENDING, ProductionAssignmentStatus.ACTIVE))
                .orElse(null);

        Workshop workshop = order.getCurrentWorkshop();
        return ProductionOrderResponse.builder()
                .id(order.getId())
                .saleOrderId(saleOrder != null ? saleOrder.getId() : null)
                .clientFullName(clientName)
                .productionStatus(order.getProductionStatus())
                .currentWorkshopId(workshop != null ? workshop.getId() : null)
                .currentWorkshopName(workshop != null ? workshop.getName() : null)
                .currentAssignmentStatus(current != null ? current.getAssignmentStatus() : null)
                .currentAssignmentId(current != null ? current.getId() : null)
                .startedAt(order.getStartedAt())
                .doneAt(order.getDoneAt())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private ProductionEventResponse toEventResponse(ProductionEvent event) {
        User actor = event.getActor();
        Workshop from = event.getFromWorkshop();
        Workshop to = event.getToWorkshop();
        return ProductionEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .fromWorkshopId(from != null ? from.getId() : null)
                .fromWorkshopName(from != null ? from.getName() : null)
                .toWorkshopId(to != null ? to.getId() : null)
                .toWorkshopName(to != null ? to.getName() : null)
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actor != null
                        ? (actor.getFullName() != null ? actor.getFullName() : actor.getUsername())
                        : null)
                .occurredAt(event.getOccurredAt())
                .comment(event.getComment())
                .build();
    }
}
