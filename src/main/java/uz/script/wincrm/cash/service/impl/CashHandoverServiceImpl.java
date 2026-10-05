package uz.script.wincrm.cash.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cash.CashHandover;
import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.cash.dto.CashHandoverRequest;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.cash.response.CashHandoverResponse;
import uz.script.wincrm.cash.response.CashHandoverSummaryResponse;
import uz.script.wincrm.cash.service.CashHandoverService;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.suppliers.repository.SupplierPaymentRepository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashHandoverServiceImpl implements CashHandoverService {

    private static final String REVIEW_AUTHORITY = "CASH_HANDOVER_EDIT";
    private static final int DEFAULT_LIST_DAYS = 30;

    private final CashHandoverRepository repository;
    private final PaymentRepository paymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CashHandoverSummaryResponse> summary(LocalDate date, Long userId) {
        LocalDate day = date != null ? date : LocalDate.now();
        Long cashierId = canReview() && userId != null ? userId : currentDetails().getId();
        return buildSummary(cashierId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashHandoverResponse> list(LocalDate fromDate, LocalDate toDate, CashHandoverStatus status, Long userId) {
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.minusDays(DEFAULT_LIST_DAYS);
        if (from.isAfter(to)) {
            throw new BadRequestException("fromDate toDate'dan katta bo'lishi mumkin emas");
        }
        Long currentId = currentDetails().getId();
        Long cashierId = canReview() ? userId : currentId;
        return repository.findForList(from, to, cashierId).stream()
                .filter(h -> status == null || h.getHandoverStatus() == status)
                .map(h -> toResponse(h, currentId))
                .toList();
    }

    @Override
    @Transactional
    public List<CashHandoverResponse> create(CashHandoverRequest request) {
        LocalDate day = request.getHandoverDate() != null ? request.getHandoverDate() : LocalDate.now();
        if (day.isAfter(LocalDate.now())) {
            throw new BadRequestException("Kelajakdagi sana uchun topshirib bo'lmaydi");
        }
        Set<Long> seen = new HashSet<>();
        for (CashHandoverRequest.Item item : request.getItems()) {
            if (!seen.add(item.getPaymentTypeId())) {
                throw new BadRequestException("Bitta kassa bir necha marta ko'rsatilgan");
            }
        }

        User cashier = currentUser();
        Map<String, CashHandoverSummaryResponse> summary = buildSummary(cashier.getId(), day).stream()
                .collect(Collectors.toMap(s -> key(s.getPaymentTypeId(), s.getCurrency()), Function.identity()));
        String comment = blankToNull(request.getComment());

        List<CashHandover> saved = new ArrayList<>();
        for (CashHandoverRequest.Item item : request.getItems()) {
            PaymentType type = paymentTypeRepository.findById(item.getPaymentTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment type not found with id: " + item.getPaymentTypeId()));
            Currency currency = CurrencyMath.orBase(type.getCurrency());
            CashHandoverSummaryResponse row = summary.get(key(type.getId(), currency));
            BigDecimal expected = row != null ? row.getRemaining() : BigDecimal.ZERO;

            saved.add(repository.save(CashHandover.builder()
                    .cashier(cashier)
                    .paymentType(type)
                    .handoverDate(day)
                    .currency(currency)
                    .expectedAmount(expected)
                    .amount(item.getAmount())
                    .handoverStatus(CashHandoverStatus.PENDING)
                    .comment(comment)
                    .filial(type.getFilial())
                    .build()));
        }
        log.info("User {} submitted {} cash handover(s) for {}", cashier.getId(), saved.size(), day);
        return saved.stream().map(h -> toResponse(h, cashier.getId())).toList();
    }

    @Override
    @Transactional
    public CashHandoverResponse accept(Long id, String comment) {
        return decide(id, CashHandoverStatus.ACCEPTED, comment);
    }

    @Override
    @Transactional
    public CashHandoverResponse reject(Long id, String comment) {
        return decide(id, CashHandoverStatus.REJECTED, comment);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        CashHandover handover = findPending(id);
        if (!handover.getCashier().getId().equals(currentDetails().getId())) {
            throw new ForbiddenException("Faqat o'z topshirishingizni bekor qila olasiz");
        }
        handover.setStatus(Status.DELETED);
        repository.save(handover);
        log.info("Cash handover {} cancelled by its cashier", id);
    }

    private CashHandoverResponse decide(Long id, CashHandoverStatus decision, String comment) {
        CashHandover handover = findPending(id);
        CustomUserDetails details = currentDetails();
        if (handover.getCashier().getId().equals(details.getId()) && !details.isSuperAdmin()) {
            throw new ForbiddenException("O'z topshirishingizni o'zingiz qabul qila olmaysiz");
        }
        handover.setHandoverStatus(decision);
        handover.setReviewer(currentUser());
        handover.setReviewedAt(LocalDateTime.now());
        handover.setReviewComment(blankToNull(comment));
        repository.save(handover);
        log.info("Cash handover {} {} by user {}", id, decision, details.getId());
        return toResponse(handover, details.getId());
    }

    private CashHandover findPending(Long id) {
        CashHandover handover = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash handover not found with id: " + id));
        if (handover.getHandoverStatus() != CashHandoverStatus.PENDING) {
            throw new BadRequestException("Topshirish allaqachon ko'rib chiqilgan");
        }
        return handover;
    }

    private List<CashHandoverSummaryResponse> buildSummary(Long cashierId, LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        Map<String, BigDecimal> incoming = sums(paymentRepository.sumCashByTypeForUser(cashierId, start, end));
        Map<String, BigDecimal> outgoing = sums(supplierPaymentRepository.sumCashByTypeForCreator(cashierId, start, end));
        Map<String, BigDecimal> pending = new HashMap<>();
        Map<String, BigDecimal> accepted = new HashMap<>();
        for (Object[] row : repository.sumForCashierAndDate(cashierId, day,
                List.of(CashHandoverStatus.PENDING, CashHandoverStatus.ACCEPTED))) {
            String key = key((Long) row[0], CurrencyMath.orBase((Currency) row[1]));
            Map<String, BigDecimal> target = row[2] == CashHandoverStatus.PENDING ? pending : accepted;
            target.merge(key, (BigDecimal) row[3], BigDecimal::add);
        }

        List<PaymentType> types = paymentTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentType::getId))
                .toList();
        Map<Long, PaymentType> typeById = types.stream().collect(Collectors.toMap(PaymentType::getId, Function.identity()));

        Set<String> keys = new LinkedHashSet<>();
        types.forEach(t -> keys.add(key(t.getId(), CurrencyMath.orBase(t.getCurrency()))));
        keys.addAll(incoming.keySet());
        keys.addAll(outgoing.keySet());
        keys.addAll(pending.keySet());
        keys.addAll(accepted.keySet());

        List<CashHandoverSummaryResponse> result = new ArrayList<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            PaymentType type = typeById.get(Long.valueOf(parts[0]));
            if (type == null) {
                continue;
            }
            BigDecimal in = incoming.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal out = outgoing.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal pend = pending.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal acc = accepted.getOrDefault(key, BigDecimal.ZERO);
            result.add(CashHandoverSummaryResponse.builder()
                    .paymentTypeId(type.getId())
                    .paymentTypeName(type.getName())
                    .currency(Currency.valueOf(parts[1]))
                    .incoming(in)
                    .outgoing(out)
                    .pending(pend)
                    .accepted(acc)
                    .remaining(in.subtract(out).subtract(pend).subtract(acc))
                    .build());
        }
        return result;
    }

    private CashHandoverResponse toResponse(CashHandover h, Long currentUserId) {
        User cashier = h.getCashier();
        User reviewer = h.getReviewer();
        return CashHandoverResponse.builder()
                .id(h.getId())
                .handoverDate(h.getHandoverDate())
                .cashierId(cashier.getId())
                .cashierName(displayName(cashier))
                .paymentTypeId(h.getPaymentType().getId())
                .paymentTypeName(h.getPaymentType().getName())
                .currency(h.getCurrency())
                .expectedAmount(h.getExpectedAmount())
                .amount(h.getAmount())
                .difference(h.getAmount().subtract(h.getExpectedAmount()))
                .handoverStatus(h.getHandoverStatus())
                .comment(h.getComment())
                .reviewerId(reviewer != null ? reviewer.getId() : null)
                .reviewerName(reviewer != null ? displayName(reviewer) : null)
                .reviewedAt(h.getReviewedAt())
                .reviewComment(h.getReviewComment())
                .createdAt(h.getCreatedAt())
                .mine(cashier.getId().equals(currentUserId))
                .build();
    }

    private static Map<String, BigDecimal> sums(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.merge(key((Long) row[0], CurrencyMath.orBase((Currency) row[1])), (BigDecimal) row[2], BigDecimal::add);
        }
        return map;
    }

    private static String key(Long typeId, Currency currency) {
        return typeId + ":" + currency.name();
    }

    private static String displayName(User user) {
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean canReview() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> REVIEW_AUTHORITY.equals(a.getAuthority()));
    }

    private CustomUserDetails currentDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new ForbiddenException("Foydalanuvchi aniqlanmadi");
        }
        return details;
    }

    private User currentUser() {
        Long id = currentDetails().getId();
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
