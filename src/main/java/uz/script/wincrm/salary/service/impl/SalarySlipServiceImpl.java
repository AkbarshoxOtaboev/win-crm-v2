package uz.script.wincrm.salary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.repository.SalaryConfigRepository;
import uz.script.wincrm.salary.repository.SalaryTransactionRepository;
import uz.script.wincrm.salary.response.SalarySlipAggregate;
import uz.script.wincrm.salary.response.SalarySlipResponse;
import uz.script.wincrm.salary.service.SalarySlipService;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalarySlipServiceImpl implements SalarySlipService {

    private final SalaryTransactionRepository transactionRepository;
    private final SalaryConfigRepository configRepository;
    private final UserRepository userRepository;

    @Override
    public SalarySlipResponse getSlip(Long userId, Integer year, Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Davr uchun fiksa: o'sha oyning oxirgi kunida amalda bo'lgan konfiguratsiyadan.
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();
        Optional<SalaryConfig> configOpt = configRepository.findEffectiveConfig(userId, periodEnd);
        BigDecimal baseSalary = configOpt.map(SalaryConfig::getBaseSalary).orElse(BigDecimal.ZERO);

        SalarySlipAggregate agg = transactionRepository.aggregateForPeriod(userId, year, month);
        BigDecimal commission = safe(agg != null ? agg.getTotalCommission() : null);
        BigDecimal commissionReversal = safe(agg != null ? agg.getTotalCommissionReversal() : null);
        BigDecimal bonus = safe(agg != null ? agg.getTotalBonus() : null);
        BigDecimal deduction = safe(agg != null ? agg.getTotalDeduction() : null);
        BigDecimal advance = safe(agg != null ? agg.getTotalAdvance() : null);

        BigDecimal net = baseSalary
                .add(commission)
                .subtract(commissionReversal)
                .add(bonus)
                .subtract(deduction)
                .subtract(advance);

        return SalarySlipResponse.builder()
                .userId(userId)
                .userFullName(user.getFullName())
                .periodYear(year)
                .periodMonth(month)
                .baseSalary(baseSalary)
                .totalCommission(commission)
                .totalCommissionReversal(commissionReversal)
                .totalBonus(bonus)
                .totalDeduction(deduction)
                .totalAdvance(advance)
                .netSalary(net)
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
