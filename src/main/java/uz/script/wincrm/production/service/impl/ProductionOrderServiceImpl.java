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
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.dto.SaleOrderHistoryDTO;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.repository.SaleOrderImageRepository;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.service.SaleOrderHistoryService;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;
import uz.script.wincrm.workshop.repository.WorkshopRepository;
import uz.script.wincrm.workshop.service.WorkshopAccess;
import uz.script.wincrm.workshop.service.WorkshopBalanceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final WorkshopAccess workshopAccess;
    private final SaleOrderItemRepository saleOrderItemRepository;
    private final SaleOrderImageRepository saleOrderImageRepository;

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

        List<Workshop> route = resolveRoute(dto);
        Workshop workshop = route.get(0);
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
                .route(route)
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
    public ProductionOrderResponse findBySaleOrderId(Long saleOrderId) {
        return productionOrderRepository.findBySaleOrder_Id(saleOrderId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    public List<ProductionOrderResponse> fetchAll() {
        return productionOrderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Sex doskasi: kutilayotgan va jarayondagi topshiriqlar doim, yakunlanganlari esa
     * [fromDate, toDate] oralig'ida tugatilganlari — barchasi sexga kelgan tartibda.
     */
    @Override
    public List<ProductionOrderResponse> board(Long workshopId, LocalDate fromDate, LocalDate toDate) {
        workshopAccess.assertWorkshop(workshopId);
        getActiveWorkshop(workshopId);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("Boshlanish sanasi tugash sanasidan keyin bo'lishi mumkin emas");
        }
        List<ProductionAssignment> rows = new ArrayList<>(assignmentRepository
                .findByWorkshop_IdAndAssignmentStatusInOrderByCreatedAtAsc(
                        workshopId,
                        List.of(ProductionAssignmentStatus.PENDING, ProductionAssignmentStatus.ACTIVE)));
        rows.addAll(assignmentRepository.findDoneBetween(workshopId, fromDate, toDate));
        return rows.stream()
                .filter(a -> a.getProductionOrder().getProductionStatus() != ProductionOrderStatus.CANCELLED)
                .sorted(Comparator.comparing(ProductionAssignment::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toBoardResponse)
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
        workshopAccess.assertWorkshop(current.getWorkshop().getId());
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
        workshopAccess.assertWorkshop(current.getWorkshop().getId());
        Workshop next = getActiveWorkshop(dto.getNextWorkshopId());
        if (current.getWorkshop().getId().equals(next.getId())) {
            throw new BadRequestException("Keyingi sex joriydan farq qilishi kerak");
        }

        moveToNextWorkshop(order, current, next, dto.getNote());
        return toResponse(order);
    }

    private void moveToNextWorkshop(ProductionOrder order, ProductionAssignment current, Workshop next, String note) {
        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();

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
                actor, now, note);
        recordEvent(order, ProductionEventType.ASSIGNED, current.getWorkshop(), next,
                actor, now, null);
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
        workshopAccess.assertWorkshop(current.getWorkshop().getId());
        String note = dto != null ? dto.getNote() : null;

        Workshop plannedNext = plannedNextWorkshop(order, current);
        if (plannedNext != null) {
            moveToNextWorkshop(order, current, getActiveWorkshop(plannedNext.getId()), note);
            return toResponse(order);
        }

        LocalDateTime now = LocalDateTime.now();
        User actor = currentUser();

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

    private List<Workshop> resolveRoute(SendToProductionDTO dto) {
        List<Long> ids = dto.getWorkshopIds() != null && !dto.getWorkshopIds().isEmpty()
                ? dto.getWorkshopIds()
                : (dto.getWorkshopId() != null ? List.of(dto.getWorkshopId()) : List.of());
        if (ids.isEmpty()) {
            throw new BadRequestException("Kamida bitta sex tanlang");
        }
        List<Workshop> route = new ArrayList<>();
        for (Long workshopId : ids) {
            if (workshopId == null) {
                throw new BadRequestException("Sexlar ketma-ketligida bo'sh qadam bor");
            }
            Workshop workshop = getActiveWorkshop(workshopId);
            if (!route.isEmpty() && route.get(route.size() - 1).getId().equals(workshop.getId())) {
                throw new BadRequestException("Bir xil sex ketma-ket ikki marta tanlanmasligi kerak: " + workshop.getName());
            }
            route.add(workshop);
        }
        return route;
    }

    /**
     * Joriy sexdan keyingi rejalashtirilgan sex. Joriy assignment sequenceNo'si marshrutdagi
     * keyingi qadam indeksiga teng (1-qadam tugasa - 2-qadam, ya'ni index 1).
     */
    private Workshop plannedNextWorkshop(ProductionOrder order, ProductionAssignment current) {
        List<Workshop> route = order.getRoute();
        if (route == null || route.isEmpty()) {
            return null;
        }
        int idx = current.getSequenceNo() != null ? current.getSequenceNo() : route.size();
        Long currentWorkshopId = current.getWorkshop().getId();
        while (idx < route.size() && route.get(idx).getId().equals(currentWorkshopId)) {
            idx++;
        }
        return idx < route.size() ? route.get(idx) : null;
    }

    private List<ProductionOrderResponse.RouteStep> buildRoute(ProductionOrder order, ProductionAssignment open) {
        List<Workshop> route = order.getRoute();
        if (route == null || route.isEmpty()) {
            return List.of();
        }
        boolean done = order.getProductionStatus() == ProductionOrderStatus.DONE;
        int currentIdx = -1;
        int doneBefore;
        if (open != null) {
            int base = open.getSequenceNo() != null ? open.getSequenceNo() - 1 : 0;
            for (int i = Math.max(base, 0); i < route.size(); i++) {
                if (route.get(i).getId().equals(open.getWorkshop().getId())) {
                    currentIdx = i;
                    break;
                }
            }
            doneBefore = currentIdx >= 0 ? currentIdx : base;
        } else {
            doneBefore = assignmentRepository.countByProductionOrder_Id(order.getId());
        }

        List<ProductionOrderResponse.RouteStep> steps = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            String state;
            if (done || i < doneBefore) {
                state = "DONE";
            } else if (i == currentIdx) {
                state = "CURRENT";
            } else {
                state = "PLANNED";
            }
            Workshop w = route.get(i);
            steps.add(ProductionOrderResponse.RouteStep.builder()
                    .stepNo(i + 1)
                    .workshopId(w.getId())
                    .workshopName(w.getName())
                    .state(state)
                    .build());
        }
        return steps;
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
        Workshop next = current != null ? plannedNextWorkshop(order, current) : null;
        return ProductionOrderResponse.builder()
                .route(buildRoute(order, current))
                .nextWorkshopId(next != null ? next.getId() : null)
                .nextWorkshopName(next != null ? next.getName() : null)
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

    private ProductionOrderResponse toBoardResponse(ProductionAssignment assignment) {
        ProductionOrderResponse response = toResponse(assignment.getProductionOrder());
        response.setBoardAssignmentId(assignment.getId());
        response.setBoardAssignmentStatus(assignment.getAssignmentStatus());
        response.setAcceptedAt(assignment.getStartedAt());
        response.setSubmittedAt(assignment.getFinishedAt());

        SaleOrder saleOrder = assignment.getProductionOrder().getSaleOrder();
        if (saleOrder == null) {
            return response;
        }
        response.setOrderDate(saleOrder.getOrderDate());
        response.setPlannedReadyDate(saleOrder.getPlannedReadyDate());
        response.setSaleOrderComment(saleOrder.getComment());
        response.setItems(saleOrderItemRepository.findAllBySaleOrderId(saleOrder.getId()).stream()
                .filter(i -> i.getStatus() == Status.ACTIVE)
                .map(this::toBoardItem)
                .toList());
        response.setImages(saleOrderImageRepository.findBySaleOrderIdOrderByCreatedAtAsc(saleOrder.getId()).stream()
                .filter(img -> img.getStatus() == Status.ACTIVE && img.getFileName() != null)
                .map(img -> ProductionOrderResponse.Image.builder()
                        .id(img.getId())
                        .url("/api/files/" + img.getFileName())
                        .originalFileName(img.getOriginalFileName())
                        .build())
                .toList());
        return response;
    }

    private ProductionOrderResponse.Item toBoardItem(SaleOrderItem item) {
        BigDecimal pieces = null;
        if (item.getWidth() != null && item.getHeight() != null && item.getCount() != null
                && item.getWidth().signum() > 0 && item.getHeight().signum() > 0) {
            BigDecimal area = item.getWidth().multiply(item.getHeight())
                    .divide(BigDecimal.valueOf(10000), 6, RoundingMode.HALF_UP);
            pieces = item.getCount().divide(area, 0, RoundingMode.HALF_UP);
        }
        return ProductionOrderResponse.Item.builder()
                .id(item.getId())
                .goodsId(item.getGoods() != null ? item.getGoods().getId() : null)
                .goodsName(item.getGoods() != null ? item.getGoods().getName() : null)
                .width(item.getWidth())
                .height(item.getHeight())
                .count(item.getCount())
                .pieces(pieces)
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
