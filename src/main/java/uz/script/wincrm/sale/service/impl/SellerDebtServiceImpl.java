package uz.script.wincrm.sale.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyAmount;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.response.SellerDebtResponse;
import uz.script.wincrm.sale.service.SellerDebtService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerDebtServiceImpl implements SellerDebtService {

    private static final String VIEW_ALL_AUTHORITY = "DEBT_NOTIFICATION_VIEW";

    private final SaleOrderRepository saleOrderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SellerDebtResponse> fetchSellerDebts(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("startDate endDate'dan katta bo'lishi mumkin emas");
        }

        Long effectiveUserId = canViewAll() ? userId : currentUser().getId();

        Map<Long, List<SaleOrder>> bySeller = saleOrderRepository.findDebtOrders(start, end, effectiveUserId)
                .stream()
                .collect(Collectors.groupingBy(o -> o.getUser().getId(), LinkedHashMap::new, Collectors.toList()));

        return bySeller.values().stream()
                .map(this::toSellerResponse)
                .sorted(Comparator.comparing(SellerDebtResponse::getTotalDebt).reversed())
                .toList();
    }

    private SellerDebtResponse toSellerResponse(List<SaleOrder> orders) {
        User seller = orders.getFirst().getUser();

        Map<String, List<SaleOrder>> byClientAndCurrency = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> (o.getClient() != null ? o.getClient().getId() : 0L) + ":" + o.currencyOrBase(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SellerDebtResponse.ClientDebt> clients = byClientAndCurrency.values().stream()
                .map(this::toClientDebt)
                .sorted(Comparator.comparing(SellerDebtResponse.ClientDebt::getCurrency)
                        .thenComparing(SellerDebtResponse.ClientDebt::getDebt, Comparator.reverseOrder()))
                .toList();

        Map<Currency, BigDecimal> debtsByCurrency = new EnumMap<>(Currency.class);
        clients.forEach(c -> debtsByCurrency.merge(c.getCurrency(), c.getDebt(), BigDecimal::add));
        List<CurrencyAmount> debts = debtsByCurrency.entrySet().stream()
                .map(e -> new CurrencyAmount(e.getKey(), e.getValue()))
                .toList();

        return SellerDebtResponse.builder()
                .userId(seller.getId())
                .userFullName(seller.getFullName() != null ? seller.getFullName() : seller.getUsername())
                .totalDebt(debtsByCurrency.getOrDefault(Currency.BASE, BigDecimal.ZERO))
                .debts(debts)
                .clients(clients)
                .build();
    }

    private SellerDebtResponse.ClientDebt toClientDebt(List<SaleOrder> orders) {
        Client client = orders.getFirst().getClient();
        Currency currency = orders.getFirst().currencyOrBase();

        List<SellerDebtResponse.OrderDebt> orderDebts = orders.stream()
                .sorted(Comparator.comparing(SaleOrder::getOrderDate))
                .map(o -> SellerDebtResponse.OrderDebt.builder()
                        .saleOrderId(o.getId())
                        .orderDate(o.getOrderDate())
                        .totalSum(o.getTotalSum())
                        .paidSum(orZero(o.getPaidSum()))
                        .debtSum(orZero(o.getDebtSum()))
                        .currency(currency)
                        .status(o.getSalesOrderStatus())
                        .build())
                .toList();

        return SellerDebtResponse.ClientDebt.builder()
                .clientId(client != null ? client.getId() : null)
                .clientFullName(client != null ? client.getFullName() : null)
                .phone(client != null ? client.getPhone() : null)
                .currency(currency)
                .totalSum(sum(orderDebts.stream().map(SellerDebtResponse.OrderDebt::getTotalSum).toList()))
                .paidSum(sum(orderDebts.stream().map(SellerDebtResponse.OrderDebt::getPaidSum).toList()))
                .debt(sum(orderDebts.stream().map(SellerDebtResponse.OrderDebt::getDebtSum).toList()))
                .orders(orderDebts)
                .build();
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean canViewAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> VIEW_ALL_AUTHORITY.equals(a.getAuthority()));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
