package uz.script.wincrm.salary.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.SalaryTransaction;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.salary.enums.SalaryEntryType;
import uz.script.wincrm.salary.repository.SalaryConfigRepository;
import uz.script.wincrm.salary.repository.SalaryTransactionRepository;
import uz.script.wincrm.salary.service.SalaryCommissionService;
import uz.script.wincrm.users.User;

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

    private final SalaryConfigRepository configRepository;
    private final SalaryTransactionRepository transactionRepository;

    @Override
    public void recalculateCommissionForSaleOrder(SaleOrder saleOrder) {
        if (saleOrder == null || saleOrder.getId() == null) {
            return;
        }

        // Komissiya SOTUVCHIGA tegishli - buyurtmani ochgan xodim (saleOrder.user),
        // to'lovni kiritgan kassir (payment.user) EMAS.
        User seller = saleOrder.getUser();
        if (seller == null || seller.getId() == null) {
            log.debug("SaleOrder {} has no seller, skipping commission", saleOrder.getId());
            return;
        }
        Long userId = seller.getId();

        // Buyurtma sanasida amalda bo'lgan konfiguratsiyani topamiz (effective-dated stavka).
        LocalDate refDate = saleOrder.getOrderDate() != null
                ? saleOrder.getOrderDate().toLocalDate()
                : LocalDate.now();

        Optional<SalaryConfig> configOpt = configRepository.findEffectiveConfig(userId, refDate);
        if (configOpt.isEmpty()) {
            // Zaxira: agar o'sha sanaga aniq mos kelmasa, hozir amaldagi ochiq konfiguratsiya.
            configOpt = configRepository.findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(userId);
        }
        if (configOpt.isEmpty()) {
            log.debug("No salary config for user {}, skipping commission", userId);
            return;
        }
        SalaryConfig config = configOpt.get();

        BigDecimal paidSum = saleOrder.getPaidSum() != null ? saleOrder.getPaidSum() : BigDecimal.ZERO;
        BigDecimal totalSum = saleOrder.getTotalSum() != null ? saleOrder.getTotalSum() : BigDecimal.ZERO;

        BigDecimal expected = computeExpectedCommission(config, paidSum, totalSum);

        // Ayni paytda kitobga olingan sof komissiya (COMMISSION - COMMISSION_REVERSAL).
        BigDecimal booked = transactionRepository.netBookedCommissionBySaleOrderId(saleOrder.getId());
        if (booked == null) {
            booked = BigDecimal.ZERO;
        }

        BigDecimal delta = expected.subtract(booked);
        if (delta.signum() == 0) {
            return; // o'zgarish yo'q - idempotent
        }

        SalaryEntryType entryType = delta.signum() > 0
                ? SalaryEntryType.COMMISSION
                : SalaryEntryType.COMMISSION_REVERSAL;

        LocalDateTime now = LocalDateTime.now();

        SalaryTransaction tx = SalaryTransaction.builder()
                .userId(userId)
                .saleOrderId(saleOrder.getId())
                .entryType(entryType)
                .amount(delta.abs())
                .commissionTypeSnapshot(config.getCommissionType())
                .rateSnapshot(config.getCommissionValue())
                .baseAmountSnapshot(paidSum)
                .earnedAt(now)
                .periodYear(now.getYear())
                .periodMonth(now.getMonthValue())
                .comment(entryType == SalaryEntryType.COMMISSION_REVERSAL
                        ? "Avtomatik komissiya qaytarilishi (buyurtma #" + saleOrder.getId() + ")"
                        : "Avtomatik komissiya (buyurtma #" + saleOrder.getId() + ")")
                .build();

        transactionRepository.save(tx);
        log.info("Commission {} {} for user {} on saleOrder {}", entryType, delta.abs(), userId, saleOrder.getId());
    }

    /**
     * Trigger = "to'lov kelganda", shu sababli:
     *   PERCENT - to'langan qismdan (paidSum) foiz. Buyurtma to'liq to'langanda yig'indi
     *             avtomatik buyurtma summasining foiziga teng bo'ladi.
     *   FIXED   - aniq summa har buyurtma uchun BIR MARTA, faqat to'liq to'langach
     *             (paidSum >= totalSum va totalSum > 0).
     */
    private BigDecimal computeExpectedCommission(SalaryConfig config, BigDecimal paidSum, BigDecimal totalSum) {
        BigDecimal value = config.getCommissionValue() != null ? config.getCommissionValue() : BigDecimal.ZERO;

        if (config.getCommissionType() == CommissionType.PERCENT) {
            return paidSum.multiply(value)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }

        // FIXED
        boolean fullyPaid = totalSum.signum() > 0 && paidSum.compareTo(totalSum) >= 0;
        return fullyPaid ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
