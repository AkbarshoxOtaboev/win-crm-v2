package uz.script.wincrm.payment.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.clients.repository.ClientRepository;
import uz.script.wincrm.clients.service.ClientBalanceService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
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
import uz.script.wincrm.salary.service.SalaryCommissionService;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final ExchangeRateService exchangeRateService;
    private final SalaryCommissionService salaryCommissionService;

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

            Payment draft = draftPayment(dto, paymentType, saleOrder, null);
            ensureWithinDebt(saleOrder, draft.getAppliedAmount(), null);

            first = savePayment(draft, client, user, paymentType, saleOrder);
            recalculateSaleOrderSums(saleOrder);
        } else {
            // Buyurtma ko'rsatilmasa to'lov taqsimlanmagan holda qoladi; kassir keyin akt sverkada taqsimlaydi
            first = savePayment(draftPayment(dto, paymentType, null, null), client, user, paymentType, null);
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
        Currency poolCurrency = pool.isEmpty() ? Currency.BASE : CurrencyMath.orBase(pool.get(0).getDebtCurrency());
        if (pool.stream().anyMatch(p -> CurrencyMath.orBase(p.getDebtCurrency()) != poolCurrency)) {
            throw new BadRequestException("Bir vaqtda faqat bitta valyutadagi to'lovlarni taqsimlash mumkin");
        }
        pool.sort(Comparator.comparing(Payment::getPaymentDate).thenComparing(Payment::getId));
        BigDecimal poolTotal = pool.stream()
                .map(Payment::appliedOrPaid)
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
            if (order.currencyOrBase() != poolCurrency) {
                throw new BadRequestException("Buyurtma #" + order.getId() + " " + order.currencyOrBase()
                        + " da, tanlangan to'lovlar esa " + poolCurrency + " qarzi uchun");
            }
            BigDecimal debt = remainingDebt(order, null);
            if (entry.getValue().compareTo(debt) > 0) {
                throw new BadRequestException("Buyurtma #" + order.getId() + " qolgan qarzi "
                        + CurrencyMath.format(debt, poolCurrency) + ", unga "
                        + CurrencyMath.format(entry.getValue(), poolCurrency) + " yozib bo'lmaydi");
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
                BigDecimal available = source.appliedOrPaid();
                if (available.compareTo(need) <= 0) {
                    source.setSaleOrder(order);
                    allocated.add(repository.save(source));
                    need = need.subtract(available);
                    index++;
                } else {
                    Payment part = splitOff(source, order, need);
                    source.setAppliedAmount(available.subtract(need));
                    source.setPaymentAmount(source.getPaymentAmount().subtract(part.getPaymentAmount()));
                    repository.save(source);
                    allocated.add(repository.save(part));
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

    /** {@code applied} - qarz valyutasida; kassadagi qismi to'lovning o'z kursi bilan o'giriladi. */
    private Payment splitOff(Payment source, SaleOrder order, BigDecimal applied) {
        String note = "To'lov #" + source.getId() + " dan taqsimlandi";
        String comment = source.getComment() == null || source.getComment().isBlank()
                ? note
                : source.getComment().trim() + " · " + note;
        BigDecimal received = CurrencyMath.convert(applied, source.getDebtCurrency(), source.getCurrency(),
                source.getExchangeRate());
        Payment part = Payment.builder()
                .paymentType(source.getPaymentType())
                .user(source.getUser())
                .paymentAmount(received)
                .currency(source.getCurrency())
                .debtCurrency(source.getDebtCurrency())
                .exchangeRate(source.getExchangeRate())
                .appliedAmount(applied)
                .paymentDate(source.getPaymentDate())
                .comment(comment.length() > 255 ? comment.substring(0, 255) : comment)
                .saleOrder(order)
                .client(source.getClient())
                .status(Status.ACTIVE)
                .build();
        part.setFilial(source.getFilial());
        return part;
    }

    private Payment draftPayment(PaymentDTO dto, PaymentType paymentType, SaleOrder saleOrder, Payment existing) {
        Payment entity = mapper.toEntity(dto);
        applyCurrency(entity, paymentType, saleOrder, dto.getDebtCurrency(), dto.getExchangeRate(), existing);
        return entity;
    }

    private Payment savePayment(Payment entity, Client client, User user, PaymentType paymentType, SaleOrder saleOrder) {
        entity.setClient(client);
        entity.setSaleOrder(saleOrder);
        entity.setUser(user);
        entity.setPaymentType(paymentType);
        entity.setStatus(Status.ACTIVE);
        return repository.save(entity);
    }

    /**
     * Kassa valyutasi to'lov turidan, qarz valyutasi buyurtmadan (yoki so'rovdan) olinadi. Farq qilsa,
     * kurs bilan o'giriladi: masalan 6 425 000 so'm, kurs 12 850, $ qarz - $500 yopiladi.
     * Kurs - qo'lda kiritilgan {@code manualRate}, bo'lmasa to'lov kunidagi Markaziy bank kursi.
     * {@code existing} - tahrirlanayotgan to'lov: juftlik va sana o'zgarmasa, eski kurs qoladi.
     */
    private void applyCurrency(Payment entity, PaymentType paymentType, SaleOrder saleOrder,
                               Currency requestedDebtCurrency, BigDecimal manualRate, Payment existing) {
        Currency currency = CurrencyMath.orBase(paymentType != null ? paymentType.getCurrency() : null);
        Currency debtCurrency;
        if (saleOrder != null) {
            debtCurrency = saleOrder.currencyOrBase();
        } else if (requestedDebtCurrency != null) {
            debtCurrency = requestedDebtCurrency;
        } else if (existing != null) {
            debtCurrency = CurrencyMath.orBase(existing.getDebtCurrency());
        } else {
            debtCurrency = currency;
        }

        Currency foreign = CurrencyMath.foreignOf(currency, debtCurrency);
        BigDecimal rate = BigDecimal.ONE;
        if (foreign != null && currency != debtCurrency && manualRate != null && manualRate.signum() > 0) {
            rate = manualRate;
        } else if (foreign != null && currency != debtCurrency) {
            LocalDateTime date = entity.getPaymentDate() != null ? entity.getPaymentDate() : LocalDateTime.now();
            boolean sameDay = existing != null && existing.getPaymentDate() != null
                    && existing.getPaymentDate().toLocalDate().equals(date.toLocalDate());
            if (sameDay && existing.getCurrency() == currency
                    && existing.getDebtCurrency() == debtCurrency && existing.getExchangeRate() != null) {
                rate = existing.getExchangeRate();
            } else {
                rate = exchangeRateService.rateOn(foreign, date.toLocalDate());
            }
        }

        entity.setCurrency(currency);
        entity.setDebtCurrency(debtCurrency);
        entity.setExchangeRate(rate);
        entity.setAppliedAmount(CurrencyMath.convert(entity.getPaymentAmount(), currency, debtCurrency, rate));
    }

    private void ensureWithinDebt(SaleOrder saleOrder, BigDecimal applied, Long excludePaymentId) {
        BigDecimal remaining = remainingDebt(saleOrder, excludePaymentId);
        if (applied.subtract(remaining).compareTo(CurrencyMath.TOLERANCE) >= 0) {
            throw new BadRequestException("To'lov buyurtmaning qolgan qarzidan katta. Qolgan qarz: "
                    + CurrencyMath.format(remaining, saleOrder.currencyOrBase()));
        }
    }

    /** Buyurtmaning qolgan qarzi (buyurtma valyutasida); {@code excludePaymentId} berilsa, o'sha to'lov hisobga olinmaydi. */
    private BigDecimal remainingDebt(SaleOrder saleOrder, Long excludePaymentId) {
        BigDecimal paid = repository.findBySaleOrderId(saleOrder.getId()).stream()
                .filter(p -> excludePaymentId == null || !p.getId().equals(excludePaymentId))
                .map(Payment::appliedOrPaid)
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
    public Page<PaymentResponse> search(Long clientId, Long paymentTypeId, LocalDate fromDate, LocalDate toDate,
                                        Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("Boshlanish sanasi tugash sanasidan keyin bo'lishi mumkin emas");
        }
        List<Specification<Payment>> specs = new ArrayList<>();
        if (clientId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("client").get("id"), clientId));
        }
        if (paymentTypeId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("paymentType").get("id"), paymentTypeId));
        }
        if (fromDate != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("paymentDate"), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            specs.add((root, query, cb) -> cb.lessThan(root.get("paymentDate"), toDate.plusDays(1).atStartOfDay()));
        }
        return repository.findAll(Specification.allOf(specs), pageable)
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

        Payment before = Payment.builder()
                .currency(entity.getCurrency())
                .debtCurrency(entity.getDebtCurrency())
                .exchangeRate(entity.getExchangeRate())
                .paymentDate(entity.getPaymentDate())
                .build();
        mapper.updateEntity(entity, dto);
        applyCurrency(entity, entity.getPaymentType(), saleOrder, dto.getDebtCurrency(), dto.getExchangeRate(), before);
        if (saleOrder != null) {
            ensureWithinDebt(saleOrder, entity.getAppliedAmount(), entity.getId());
        }
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
                .map(Payment::appliedOrPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debtSum = saleOrder.getSalesOrderStatus() == SalesOrderStatus.CANCELLED
                ? BigDecimal.ZERO
                : saleOrder.getTotalSum().subtract(paidSum);

        saleOrder.setPaidSum(paidSum);
        saleOrder.setDebtSum(debtSum);

        saleOrderRepository.save(saleOrder);
        salaryCommissionService.recalculateCommissionForSaleOrder(saleOrder);
    }
}