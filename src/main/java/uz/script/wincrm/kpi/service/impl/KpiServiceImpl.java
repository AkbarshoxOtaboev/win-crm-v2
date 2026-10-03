package uz.script.wincrm.kpi.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.kpi.KpiEntry;
import uz.script.wincrm.kpi.KpiRate;
import uz.script.wincrm.kpi.repository.KpiEntryRepository;
import uz.script.wincrm.kpi.repository.KpiRateRepository;
import uz.script.wincrm.kpi.response.KpiEntryResponse;
import uz.script.wincrm.kpi.response.KpiRateResponse;
import uz.script.wincrm.kpi.response.KpiSummaryResponse;
import uz.script.wincrm.kpi.service.KpiService;
import uz.script.wincrm.roles.RoleResponse;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.service.SaleOrderBaseConverter;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.users.response.UserResponse;
import uz.script.wincrm.users.service.UserService;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.WorkshopBalanceEntry;
import uz.script.wincrm.workshop.repository.WorkshopBalanceEntryRepository;
import uz.script.wincrm.workshop.repository.WorkshopRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KpiServiceImpl implements KpiService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String EXCLUDED_ROLE = "SUPER_ADMIN";

    private final KpiRateRepository rateRepository;
    private final KpiEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WorkshopRepository workshopRepository;
    private final WorkshopBalanceEntryRepository workshopEntryRepository;
    private final SaleOrderBaseConverter baseConverter;

    @Override
    public void accrueForSaleOrder(SaleOrder saleOrder) {
        User seller = saleOrder.getUser();
        if (seller == null) {
            return;
        }
        KpiRate rate = rateRepository.findByUser_Id(seller.getId()).orElse(null);
        if (rate == null || rate.getPercent() == null || rate.getPercent().signum() <= 0) {
            return;
        }
        if (entryRepository.existsBySaleOrder_IdAndUser_Id(saleOrder.getId(), seller.getId())) {
            return;
        }
        SaleOrderBaseConverter.Conversion fx = baseConverter.convertToday(saleOrder, saleOrder.totalSumWithoutDelivery());
        BigDecimal base = fx.baseAmount().max(BigDecimal.ZERO);
        BigDecimal amount = base.multiply(rate.getPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();

        entryRepository.save(KpiEntry.builder()
                .user(seller)
                .saleOrder(saleOrder)
                .baseAmount(base)
                .sourceCurrency(fx.currency())
                .sourceAmount(fx.sourceAmount().max(BigDecimal.ZERO))
                .exchangeRate(fx.rate())
                .percent(rate.getPercent())
                .amount(amount)
                .earnedAt(now)
                .periodYear(now.getYear())
                .periodMonth(now.getMonthValue())
                .filial(saleOrder.getFilial())
                .build());
        log.info("KPI {} accrued to user {} for sale order {}", amount, seller.getId(), saleOrder.getId());
    }

    @Override
    public void removeForSaleOrder(Long saleOrderId) {
        for (KpiEntry entry : entryRepository.findAllBySaleOrder_Id(saleOrderId)) {
            entry.setStatus(Status.DELETED);
            entryRepository.save(entry);
        }
    }

    @Override
    public List<KpiRateResponse> fetchRates() {
        Map<Long, BigDecimal> rates = new HashMap<>();
        for (KpiRate rate : rateRepository.findAll()) {
            rates.put(rate.getUser().getId(), rate.getPercent());
        }
        return kpiUsers().stream()
                .map(u -> toRateResponse(u, rates.get(u.getId())))
                .toList();
    }

    @Override
    public KpiRateResponse setRate(Long userId, BigDecimal percent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (percent != null && (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0)) {
            throw new BadRequestException("KPI foizi 0 dan 100 gacha bo'lishi kerak");
        }
        KpiRate existing = rateRepository.findByUser_Id(userId).orElse(null);
        if (percent == null || percent.signum() == 0) {
            if (existing != null) {
                rateRepository.delete(existing);
            }
            percent = null;
        } else {
            KpiRate rate = existing != null ? existing : KpiRate.builder().user(user).build();
            rate.setPercent(percent);
            rateRepository.save(rate);
        }
        log.info("KPI rate for user {} set to {}", userId, percent);
        return KpiRateResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .percent(percent)
                .build();
    }

    @Override
    public List<KpiSummaryResponse> summary(int year, int month) {
        validateMonth(month);
        Map<Long, BigDecimal> rates = new HashMap<>();
        for (KpiRate rate : rateRepository.findAll()) {
            rates.put(rate.getUser().getId(), rate.getPercent());
        }
        Map<Long, Object[]> period = new HashMap<>();
        for (Object[] row : entryRepository.aggregateForPeriod(year, month)) {
            period.put((Long) row[0], row);
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Object[] row : entryRepository.totalsByUser()) {
            totals.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<KpiSummaryResponse> result = new ArrayList<>();
        for (UserResponse user : kpiUsers()) {
            Long id = user.getId();
            if (!rates.containsKey(id) && !totals.containsKey(id)) {
                continue;
            }
            Object[] p = period.get(id);
            result.add(KpiSummaryResponse.builder()
                    .id(id)
                    .name(user.getFullName())
                    .roles(roleNames(user))
                    .percent(rates.get(id))
                    .periodCount(p != null ? (Long) p[1] : 0L)
                    .periodAmount(p != null ? (BigDecimal) p[2] : BigDecimal.ZERO)
                    .totalAmount(totals.getOrDefault(id, BigDecimal.ZERO))
                    .build());
        }
        result.sort(Comparator.comparing(KpiSummaryResponse::getPeriodAmount).reversed()
                .thenComparing(KpiSummaryResponse::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return result;
    }

    @Override
    public List<KpiEntryResponse> userEntries(Long userId, int year, Integer month) {
        List<KpiEntry> entries;
        if (month == null) {
            entries = entryRepository.findAllByUser_IdOrderByEarnedAtAscIdAsc(userId);
        } else {
            validateMonth(month);
            entries = entryRepository.findAllByUser_IdAndPeriodYearAndPeriodMonthOrderByEarnedAtAscIdAsc(userId, year, month);
        }
        return entries.stream()
                .map(e -> KpiEntryResponse.builder()
                        .id(e.getId())
                        .earnedAt(e.getEarnedAt())
                        .saleOrderId(e.getSaleOrder().getId())
                        .clientFullName(clientName(e.getSaleOrder()))
                        .baseAmount(e.getBaseAmount())
                        .sourceCurrency(e.getSourceCurrency())
                        .sourceAmount(e.getSourceAmount())
                        .exchangeRate(e.getExchangeRate())
                        .percent(e.getPercent())
                        .amount(e.getAmount())
                        .build())
                .toList();
    }

    @Override
    public List<KpiSummaryResponse> workshopSummary(int year, int month) {
        validateMonth(month);
        YearMonth ym = YearMonth.of(year, month);
        Map<Long, Object[]> period = new HashMap<>();
        for (Object[] row : workshopEntryRepository.aggregateBetween(
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay())) {
            period.put((Long) row[0], row);
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Object[] row : workshopEntryRepository.totalsByWorkshop()) {
            totals.put((Long) row[0], (BigDecimal) row[1]);
        }

        return workshopRepository.findAll().stream()
                .filter(w -> w.getStatus() != Status.DELETED)
                .sorted(Comparator.comparing(Workshop::getId))
                .map(w -> {
                    Object[] p = period.get(w.getId());
                    User manager = w.getManager();
                    return KpiSummaryResponse.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .managerName(manager != null ? manager.getFullName() : null)
                            .percent(w.getFeePercent())
                            .periodCount(p != null ? (Long) p[1] : 0L)
                            .periodAmount(p != null ? (BigDecimal) p[2] : BigDecimal.ZERO)
                            .totalAmount(totals.getOrDefault(w.getId(), BigDecimal.ZERO))
                            .build();
                })
                .toList();
    }

    @Override
    public List<KpiEntryResponse> workshopEntries(Long workshopId, int year, Integer month) {
        List<WorkshopBalanceEntry> entries;
        if (month == null) {
            entries = workshopEntryRepository.findAllByWorkshop_IdOrderByOccurredAtAscIdAsc(workshopId);
        } else {
            validateMonth(month);
            YearMonth ym = YearMonth.of(year, month);
            entries = workshopEntryRepository
                    .findAllByWorkshop_IdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(
                            workshopId, ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
        }
        return entries.stream()
                .map(e -> KpiEntryResponse.builder()
                        .id(e.getId())
                        .earnedAt(e.getOccurredAt())
                        .saleOrderId(e.getSaleOrder().getId())
                        .clientFullName(clientName(e.getSaleOrder()))
                        .baseAmount(e.getOrderTotalSum())
                        .sourceCurrency(e.getSourceCurrency())
                        .sourceAmount(e.getSourceAmount())
                        .exchangeRate(e.getExchangeRate())
                        .percent(e.getFeePercent())
                        .amount(e.getAmount())
                        .build())
                .toList();
    }

    private List<UserResponse> kpiUsers() {
        return userService.fetchAllUsers().stream()
                .filter(u -> u.getRole() == null
                        || u.getRole().stream().noneMatch(r -> EXCLUDED_ROLE.equals(r.getName())))
                .sorted(Comparator.comparing(UserResponse::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private KpiRateResponse toRateResponse(UserResponse user, BigDecimal percent) {
        return KpiRateResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .roles(roleNames(user))
                .filialName(user.getFilialName())
                .percent(percent)
                .build();
    }

    private List<String> roleNames(UserResponse user) {
        return user.getRole() == null ? List.of() : user.getRole().stream().map(RoleResponse::getName).sorted().toList();
    }

    private String clientName(SaleOrder order) {
        return order.getClient() != null ? order.getClient().getFullName() : null;
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
    }
}
