package uz.script.wincrm.discount.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.discount.DiscountRule;
import uz.script.wincrm.discount.DiscountRuleRepository;
import uz.script.wincrm.discount.dto.DiscountRuleRequest;
import uz.script.wincrm.discount.response.DiscountRuleResponse;
import uz.script.wincrm.discount.service.DiscountRuleService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.roles.RoleRepository;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DiscountRuleServiceImpl implements DiscountRuleService {

    private static final String UNRESTRICTED_ROLE = "SUPER_ADMIN";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final DiscountRuleRepository repository;
    private final RoleRepository roleRepository;

    @Override
    public List<DiscountRuleResponse> fetchAll() {
        Map<Long, DiscountRule> rules = repository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getRole().getId(), Function.identity()));
        return roleRepository.findAll().stream()
                .filter(r -> r.getStatus() != Status.DELETED)
                .sorted(Comparator.comparing(Role::getId))
                .map(role -> {
                    DiscountRule rule = rules.get(role.getId());
                    boolean locked = UNRESTRICTED_ROLE.equals(role.getName());
                    return DiscountRuleResponse.builder()
                            .roleId(role.getId())
                            .roleName(role.getName())
                            .maxDiscountPercent(locked || rule == null ? null : rule.getMaxDiscountPercent())
                            .locked(locked)
                            .build();
                })
                .toList();
    }

    @Override
    public List<DiscountRuleResponse> saveAll(DiscountRuleRequest request) {
        for (DiscountRuleRequest.Item item : request.getRules()) {
            Role role = roleRepository.findById(item.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + item.getRoleId()));
            if (UNRESTRICTED_ROLE.equals(role.getName())) {
                continue;
            }
            BigDecimal percent = item.getMaxDiscountPercent();
            if (percent != null && (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0)) {
                throw new BadRequestException("Chegirma foizi 0 dan 100 gacha bo'lishi kerak");
            }
            DiscountRule rule = repository.findByRole_Id(role.getId())
                    .orElseGet(() -> DiscountRule.builder().role(role).build());
            rule.setMaxDiscountPercent(percent);
            repository.save(rule);
        }
        log.info("Discount rules updated: {} roles", request.getRules().size());
        return fetchAll();
    }

    @Override
    public BigDecimal currentUserLimit() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        Set<Role> roles = details.getUser().getRoles();
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (roles.stream().anyMatch(r -> UNRESTRICTED_ROLE.equals(r.getName()))) {
            return null;
        }
        Map<Long, BigDecimal> limits = repository.findAllByRole_IdIn(roles.stream().map(Role::getId).toList())
                .stream()
                .filter(r -> r.getMaxDiscountPercent() != null)
                .collect(Collectors.toMap(r -> r.getRole().getId(), DiscountRule::getMaxDiscountPercent));

        // Bir nechta rol bo'lsa eng yumshoq chegara amal qiladi; chegarasiz rol - cheklovsiz.
        BigDecimal best = null;
        for (Role role : roles) {
            BigDecimal limit = limits.get(role.getId());
            if (limit == null) {
                return null;
            }
            if (best == null || limit.compareTo(best) > 0) {
                best = limit;
            }
        }
        return best;
    }

    @Override
    public void checkOrderDiscount(SaleOrder order, BigDecimal discountAmount) {
        if (order == null || discountAmount == null || discountAmount.signum() <= 0) {
            return;
        }
        BigDecimal original = order.getOriginalTotalSum();
        if (original == null || original.signum() <= 0) {
            return;
        }
        BigDecimal limit = currentUserLimit();
        if (limit == null) {
            return;
        }
        BigDecimal actual = discountAmount.multiply(HUNDRED).divide(original, 4, RoundingMode.HALF_UP);
        if (actual.compareTo(limit.add(TOLERANCE)) > 0) {
            BigDecimal maxAmount = original.multiply(limit).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            throw new BadRequestException(String.format(
                    "Sizning rolingiz uchun buyurtmaga maksimal chegirma %s%% (%s). Kiritilgan chegirma %s%%",
                    plain(limit), CurrencyMath.format(maxAmount, order.currencyOrBase()), plain(actual)));
        }
    }

    private String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
