package uz.script.wincrm.salary.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.enums.SaleType;
import uz.script.wincrm.sale.enums.SalesOrderStatus;
import uz.script.wincrm.sale.service.SaleOrderBaseConverter;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.SalaryTransaction;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.salary.enums.SalaryEntryType;
import uz.script.wincrm.salary.repository.SalaryConfigRepository;
import uz.script.wincrm.salary.repository.SalaryTransactionRepository;
import uz.script.wincrm.salary.service.SalaryCommissionService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalaryCommissionServiceImpl implements SalaryCommissionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String SELLER = "SELLER";
    private static final String TRANSPORT_MANAGER = "TRANSPORT_MANAGER";

    private final SalaryConfigRepository configRepository;
    private final SalaryTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SaleOrderBaseConverter baseConverter;

    @Override
    public void recalculateCommissionForSaleOrder(SaleOrder saleOrder) {
        if (saleOrder == null || saleOrder.getId() == null) {
            return;
        }

        User seller = saleOrder.getUser();
        if (seller == null || seller.getId() == null) {
            log.debug("SaleOrder {} has no seller, skipping commission", saleOrder.getId());
            return;
        }

        Long userId = seller.getId();
        BigDecimal booked = booked(userId, saleOrder.getId(),
                SalaryEntryType.COMMISSION, SalaryEntryType.COMMISSION_REVERSAL);

        if (!hasRole(seller, SELLER)) {
            bookDelta(userId, saleOrder.getId(), BigDecimal.ZERO, booked, null, BigDecimal.ZERO,
                    SalaryEntryType.COMMISSION, SalaryEntryType.COMMISSION_REVERSAL,
                    "Sotuvchi komissiyasi (buyurtma #" + saleOrder.getId() + ")");
            return;
        }

        SalaryConfig config = effectiveConfig(userId, referenceDate(saleOrder)).orElse(null);
        if (config == null) {
            bookDelta(userId, saleOrder.getId(), BigDecimal.ZERO, booked, null, BigDecimal.ZERO,
                    SalaryEntryType.COMMISSION, SalaryEntryType.COMMISSION_REVERSAL,
                    "Sotuvchi komissiyasi (buyurtma #" + saleOrder.getId() + ")");
            return;
        }

        BigDecimal paidSum = saleOrder.getPaidSum() != null ? saleOrder.getPaidSum() : BigDecimal.ZERO;
        BigDecimal paidBase = saleOrder.toBase(paidSum);
        BigDecimal expected = earnedBySeller(saleOrder, paidSum)
                ? sellerCommission(config, paidBase, paidSum)
                : BigDecimal.ZERO;

        bookDelta(userId, saleOrder.getId(), expected, booked, config, paidBase,
                SalaryEntryType.COMMISSION, SalaryEntryType.COMMISSION_REVERSAL,
                "Sotuvchi komissiyasi (buyurtma #" + saleOrder.getId() + ")");
    }

    @Override
    public void recalculateTransportManagerCommission(SaleOrder saleOrder, boolean delivered) {
        if (saleOrder == null || saleOrder.getId() == null) {
            return;
        }

        SaleOrderBaseConverter.Conversion fx = baseConverter.convertToday(
                saleOrder, saleOrder.getTotalSum() != null ? saleOrder.getTotalSum() : BigDecimal.ZERO);
        BigDecimal orderTotal = fx.baseAmount();

        for (User manager : userRepository.findByRoleName(TRANSPORT_MANAGER)) {
            if (!sameFilial(manager, saleOrder)) {
                continue;
            }
            Long userId = manager.getId();
            BigDecimal booked = booked(userId, saleOrder.getId(),
                    SalaryEntryType.TRANSPORT_COMMISSION, SalaryEntryType.TRANSPORT_COMMISSION_REVERSAL);
            SalaryConfig config = effectiveConfig(userId, LocalDate.now()).orElse(null);
            BigDecimal expected = delivered && config != null
                    ? transportCommission(config, orderTotal)
                    : BigDecimal.ZERO;
            bookDelta(userId, saleOrder.getId(), expected, booked, config, orderTotal,
                    SalaryEntryType.TRANSPORT_COMMISSION, SalaryEntryType.TRANSPORT_COMMISSION_REVERSAL,
                    "Yetkazish haqi (buyurtma #" + saleOrder.getId() + ")");
        }
    }

    /**
     * Chakana: DELIVERED va undan keyingi holat. Optom: WORK_DONE yetkazilgan hisoblanadi.
     * To'lov bo'lmasa yozilmaydi.
     */
    private boolean earnedBySeller(SaleOrder saleOrder, BigDecimal paidSum) {
        if (saleOrder.getStatus() != Status.ACTIVE || paidSum.signum() <= 0) {
            return false;
        }
        SalesOrderStatus status = saleOrder.getSalesOrderStatus();
        if (saleOrder.getSaleType() == SaleType.WHOLESALE) {
            return status == SalesOrderStatus.WORK_DONE || status == SalesOrderStatus.COMPLETED;
        }
        return status == SalesOrderStatus.DELIVERED
                || status == SalesOrderStatus.WORK_DONE
                || status == SalesOrderStatus.COMPLETED;
    }

    private BigDecimal sellerCommission(SalaryConfig config, BigDecimal paidBase, BigDecimal paidSum) {
        BigDecimal value = config.getCommissionValue() != null ? config.getCommissionValue() : BigDecimal.ZERO;
        if (value.signum() <= 0 || paidSum.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (config.getCommissionType() == CommissionType.PERCENT) {
            return paidBase.multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal transportCommission(SalaryConfig config, BigDecimal orderTotal) {
        BigDecimal value = config.getCommissionValue() != null ? config.getCommissionValue() : BigDecimal.ZERO;
        if (value.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (config.getCommissionType() == CommissionType.PERCENT) {
            return orderTotal.multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void bookDelta(Long userId, Long saleOrderId, BigDecimal expected, BigDecimal booked,
                           SalaryConfig config, BigDecimal baseAmount,
                           SalaryEntryType credit, SalaryEntryType debit, String comment) {
        BigDecimal delta = expected.subtract(booked);
        if (delta.signum() == 0) {
            return;
        }
        SalaryEntryType entryType = delta.signum() > 0 ? credit : debit;
        LocalDateTime now = LocalDateTime.now();
        transactionRepository.save(SalaryTransaction.builder()
                .userId(userId)
                .saleOrderId(saleOrderId)
                .entryType(entryType)
                .amount(delta.abs())
                .commissionTypeSnapshot(config != null ? config.getCommissionType() : null)
                .rateSnapshot(config != null ? config.getCommissionValue() : null)
                .baseAmountSnapshot(baseAmount)
                .earnedAt(now)
                .periodYear(now.getYear())
                .periodMonth(now.getMonthValue())
                .comment(entryType == debit ? "Qaytarilgan " + comment : comment)
                .build());
        log.info("{} {} for user {} on saleOrder {}", entryType, delta.abs(), userId, saleOrderId);
    }

    private BigDecimal booked(Long userId, Long saleOrderId, SalaryEntryType credit, SalaryEntryType debit) {
        BigDecimal booked = transactionRepository.netBookedForUserOrder(userId, saleOrderId, credit, debit);
        return booked != null ? booked : BigDecimal.ZERO;
    }

    private Optional<SalaryConfig> effectiveConfig(Long userId, LocalDate date) {
        Optional<SalaryConfig> config = configRepository.findEffectiveConfig(userId, date);
        if (config.isPresent()) {
            return config;
        }
        return configRepository.findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(userId);
    }

    private LocalDate referenceDate(SaleOrder saleOrder) {
        return saleOrder.getOrderDate() != null ? saleOrder.getOrderDate().toLocalDate() : LocalDate.now();
    }

    private boolean hasRole(User user, String role) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(r -> role.equals(r.getName()));
    }

    private boolean sameFilial(User user, SaleOrder order) {
        Long orderFilial = order.getFilial() != null ? order.getFilial().getId() : null;
        if (orderFilial == null) {
            return true;
        }
        Long userFilial = user.getFilial() != null ? user.getFilial().getId() : null;
        return orderFilial.equals(userFilial);
    }
}
