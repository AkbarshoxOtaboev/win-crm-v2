package uz.script.wincrm.payment.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.clients.repository.ClientRepository;
import uz.script.wincrm.clients.service.ClientBalanceService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.payment.Payment;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.dto.PaymentAllocationRequest;
import uz.script.wincrm.payment.dto.PaymentDTO;
import uz.script.wincrm.payment.mapper.PaymentMapper;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.payment.response.PaymentResponse;
import uz.script.wincrm.payment.service.PaymentService;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final PaymentTypeRepository paymentTypeRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientBalanceService clientBalanceService;

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "Payment"
    )
    public PaymentResponse create(PaymentDTO dto) {
        log.info("Create payment for client {}", dto.getClientId());

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.getClientId()));

        PaymentType paymentType = paymentTypeRepository.findById(dto.getPaymentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with id: " + dto.getPaymentTypeId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(()->new ResourceNotFoundException("User not found with id:  "+ dto.getUserId()));

        Payment first;
        if (dto.getSaleOrderId() != null) {
            SaleOrder saleOrder = saleOrderRepository.findById(dto.getSaleOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + dto.getSaleOrderId()));
            ensureNotCancelled(saleOrder);
            if (saleOrder.getClient() != null && !saleOrder.getClient().getId().equals(client.getId())) {
                throw new BadRequestException("Tanlangan buyurtma boshqa mijozga tegishli");
            }

            BigDecimal remainingDebt = remainingDebt(saleOrder, null);
            if (dto.getPaymentAmount().compareTo(remainingDebt) > 0) {
                throw new BadRequestException(
                        "Payment amount exceeds the remaining debt of the sale order. Remaining debt: " + remainingDebt);
            }

            first = savePayment(dto, client, user, paymentType, saleOrder, dto.getPaymentAmount(), dto.getComment());
            recalculateSaleOrderSums(saleOrder);
        } else {
            // Buyurtma ko'rsatilmasa to'lov taqsimlanmagan holda qoladi; kassir keyin akt sverkada taqsimlaydi
            first = savePayment(dto, client, user, paymentType, null, dto.getPaymentAmount(), dto.getComment());
        }

        clientBalanceService.recalculateClientBalance(client.getId());
        return mapper.toResponse(first);
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "Payment"
    )
    public List<PaymentResponse> allocate(PaymentAllocationRequest request) {
        log.info("Allocate payments {} for client {}", request.getPaymentIds(), request.getClientId());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.getClientId()));

        List<Payment> pool = new ArrayList<>();
        for (Long paymentId : new LinkedHashSet<>(request.getPaymentIds())) {
            Payment payment = repository.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
            if (payment.getClient() == null || !payment.getClient().getId().equals(client.getId())) {
                throw new BadRequestException("To'lov #" + paymentId + " boshqa mijozga tegishli");
            }
            if (payment.getSaleOrder() != null) {
                throw new BadRequestException("To'lov #" + paymentId + " allaqachon buyurtmaga taqsimlangan");
            }
            pool.add(payment);
        }
        pool.sort(Comparator.comparing(Payment::getPaymentDate).thenComparing(Payment::getId));
        BigDecimal poolTotal = pool.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> requested = new LinkedHashMap<>();
        for (PaymentAllocationRequest.Allocation allocation : request.getAllocations()) {
            requested.merge(allocation.getSaleOrderId(), allocation.getAmount(), BigDecimal::add);
        }
        BigDecimal requestedTotal = requested.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedTotal.compareTo(poolTotal) > 0) {
            throw new BadRequestException("Taqsimlanadigan summa (" + requestedTotal
                    + ") belgilangan to'lovlar summasidan (" + poolTotal + ") katta");
        }

        Map<SaleOrder, BigDecimal> targets = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            SaleOrder order = saleOrderRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + entry.getKey()));
            ensureNotCancelled(order);
            if (order.getClient() == null || !order.getClient().getId().equals(client.getId())) {
                throw new BadRequestException("Buyurtma #" + order.getId() + " boshqa mijozga tegishli");
            }
            BigDecimal debt = remainingDebt(order, null);
            if (entry.getValue().compareTo(debt) > 0) {
                throw new BadRequestException("Buyurtma #" + order.getId() + " qolgan qarzi " + debt
                        + ", unga " + entry.getValue() + " yozib bo'lmaydi");
            }
            targets.put(order, entry.getValue());
        }

        List<Payment> allocated = new ArrayList<>();
        int index = 0;
        for (Map.Entry<SaleOrder, BigDecimal> target : targets.entrySet()) {
            SaleOrder order = target.getKey();
            BigDecimal need = target.getValue();
            while (need.signum() > 0) {
                Payment source = pool.get(index);
                BigDecimal available = source.getPaymentAmount();
                if (available.compareTo(need) <= 0) {
                    source.setSaleOrder(order);
                    allocated.add(repository.save(source));
                    need = need.subtract(available);
                    index++;
                } else {
                    source.setPaymentAmount(available.subtract(need));
                    repository.save(source);
                    allocated.add(repository.save(splitOff(source, order, need)));
                    need = BigDecimal.ZERO;
                }
            }
            repository.flush();
            recalculateSaleOrderSums(order);
        }

        clientBalanceService.recalculateClientBalance(client.getId());
        return allocated.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "Payment"
    )
    public PaymentResponse unallocate(Long id) {
        log.info("Unallocate payment {}", id);

        Payment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        SaleOrder order = entity.getSaleOrder();
        if (order == null) {
            throw new BadRequestException("To'lov hech qaysi buyurtmaga taqsimlanmagan");
        }

        entity.setSaleOrder(null);
        entity = repository.saveAndFlush(entity);
        recalculateSaleOrderSums(order);
        if (entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }
        return mapper.toResponse(entity);
    }

    private Payment splitOff(Payment source, SaleOrder order, BigDecimal amount) {
        String note = "To'lov #" + source.getId() + " dan taqsimlandi";
        String comment = source.getComment() == null || source.getComment().isBlank()
                ? note
                : source.getComment().trim() + " · " + note;
        Payment part = Payment.builder()
                .paymentType(source.getPaymentType())
                .user(source.getUser())
                .paymentAmount(amount)
                .paymentDate(source.getPaymentDate())
                .comment(comment.length() > 255 ? comment.substring(0, 255) : comment)
                .saleOrder(order)
                .client(source.getClient())
                .status(Status.ACTIVE)
                .build();
        part.setFilial(source.getFilial());
        return part;
    }

    private Payment savePayment(PaymentDTO dto, Client client, User user, PaymentType paymentType,
                                SaleOrder saleOrder, BigDecimal amount, String comment) {
        Payment entity = mapper.toEntity(dto);
        entity.setPaymentAmount(amount);
        entity.setComment(comment);
        entity.setClient(client);
        entity.setSaleOrder(saleOrder);
        entity.setUser(user);
        entity.setPaymentType(paymentType);
        entity.setStatus(Status.ACTIVE);
        return repository.save(entity);
    }

    /** Buyurtmaning qolgan qarzi; {@code excludePaymentId} berilsa, o'sha to'lov hisobga olinmaydi. */
    private BigDecimal remainingDebt(SaleOrder saleOrder, Long excludePaymentId) {
        BigDecimal paid = repository.findBySaleOrderId(saleOrder.getId()).stream()
                .filter(p -> excludePaymentId == null || !p.getId().equals(excludePaymentId))
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return saleOrder.getTotalSum().subtract(paid);
    }

    private void ensureNotCancelled(SaleOrder saleOrder) {
        if (saleOrder.getSalesOrderStatus() == SalesOrderStatus.CANCELLED) {
            throw new BadRequestException("Bekor qilingan buyurtmaga to'lov qabul qilib bo'lmaydi");
        }
    }

    @Override
    public PaymentResponse findById(Long id) {
        log.info("Fetch payment by id {}", id);

        Payment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        return mapper.toResponse(entity);
    }

    @Override
    public Page<PaymentResponse> fetchAll(Pageable pageable) {
        log.info("Fetch all payments");

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<PaymentResponse> fetchByClientId(Long clientId, Pageable pageable) {
        log.info("Fetch payments by client id {}", clientId);

        return repository.findByClientId(clientId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<PaymentResponse> fetchBySaleOrderId(Long saleOrderId, Pageable pageable) {
        log.info("Fetch payments by sale order id {}", saleOrderId);

        return repository.findBySaleOrderId(saleOrderId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<PaymentResponse> fetchByPaymentTypeId(Long paymentTypeId, Pageable pageable) {
        log.info("Fetch payments by payment type id {}", paymentTypeId);

        return repository.findByPaymentTypeId(paymentTypeId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Auditable(
            action = AuditAction.UPDATE,
            entity = "Payment"
    )
    public PaymentResponse update(Long id, PaymentDTO dto) {
        log.info("Update payment with id {}", id);

        Payment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        SaleOrder oldSaleOrder = entity.getSaleOrder();
        SaleOrder saleOrder = oldSaleOrder;

        if (dto.getClientId() != null
                && (entity.getClient() == null || !dto.getClientId().equals(entity.getClient().getId()))) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + dto.getClientId()));
            entity.setClient(client);
        }

        if (dto.getSaleOrderId() != null
                && (oldSaleOrder == null || !dto.getSaleOrderId().equals(oldSaleOrder.getId()))) {
            saleOrder = saleOrderRepository.findById(dto.getSaleOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + dto.getSaleOrderId()));
            ensureNotCancelled(saleOrder);
            entity.setSaleOrder(saleOrder);
        }

        if (dto.getPaymentTypeId() != null
                && (entity.getPaymentType() == null || !dto.getPaymentTypeId().equals(entity.getPaymentType().getId()))) {
            PaymentType paymentType = paymentTypeRepository.findById(dto.getPaymentTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with id: " + dto.getPaymentTypeId()));
            entity.setPaymentType(paymentType);
        }

        Long previousClientId = entity.getClient() != null ? entity.getClient().getId() : null;

        if (dto.getPaymentAmount() != null && saleOrder != null) {
            BigDecimal remainingDebt = remainingDebt(saleOrder, entity.getId());

            if (dto.getPaymentAmount().compareTo(remainingDebt) > 0) {
                throw new BadRequestException(
                        "Payment amount exceeds the remaining debt of the sale order. Remaining debt: " + remainingDebt);
            }
        }

        mapper.updateEntity(entity, dto);
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        entity.setCreatedUsername(username);

        entity = repository.save(entity);

        if (saleOrder != null) {
            recalculateSaleOrderSums(saleOrder);
        }
        if (oldSaleOrder != null && !Objects.equals(oldSaleOrder.getId(), saleOrder != null ? saleOrder.getId() : null)) {
            recalculateSaleOrderSums(oldSaleOrder);
        }

        Long currentClientId = entity.getClient() != null ? entity.getClient().getId() : null;
        if (currentClientId != null) {
            clientBalanceService.recalculateClientBalance(currentClientId);
        }
        if (previousClientId != null && !previousClientId.equals(currentClientId)) {
            clientBalanceService.recalculateClientBalance(previousClientId);
        }

        return mapper.toResponse(entity);
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "Payment"
    )
    public void delete(Long id) {
        log.info("Delete payment with id {}", id);

        Payment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        SaleOrder saleOrder = entity.getSaleOrder();

        entity.setStatus(Status.DELETED);
        repository.saveAndFlush(entity);

        if (saleOrder != null) {
            recalculateSaleOrderSums(saleOrder);
        }
        if (entity.getClient() != null) {
            clientBalanceService.recalculateClientBalance(entity.getClient().getId());
        }
    }

    /**
     * SaleOrder ning paidSum va debtSum qiymatlarini shu order bo'yicha
     * mavjud (DELETED bo'lmagan) barcha to'lovlar asosida qayta hisoblaydi.
     * SaleOrder'ga bog'lanmagan (umumiy/avans) to'lovlar bu hisobga kirmaydi.
     */
    private void recalculateSaleOrderSums(SaleOrder saleOrder) {
        List<Payment> payments = repository.findBySaleOrderId(saleOrder.getId());

        BigDecimal paidSum = payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debtSum = saleOrder.getSalesOrderStatus() == SalesOrderStatus.CANCELLED
                ? BigDecimal.ZERO
                : saleOrder.getTotalSum().subtract(paidSum);

        saleOrder.setPaidSum(paidSum);
        saleOrder.setDebtSum(debtSum);

        saleOrderRepository.save(saleOrder);
    }
}