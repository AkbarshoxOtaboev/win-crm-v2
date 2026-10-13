package uz.script.wincrm.cash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.cash.response.CashAvailableResponse;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.suppliers.repository.SupplierPaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Har bir kassaning (to'lov turi + valyuta) joriy qoldig'i: mijozlardan kirim - yetkazib beruvchilarga chiqim -
 * rahbarga topshirilgan (qabul qilingan) - kassadan qilingan xarajatlar. Valyutalar bir-biriga qo'shilmaydi.
 */
@Service
@RequiredArgsConstructor
public class CashAvailabilityService {

    private static final LocalDate BEGINNING = LocalDate.of(2000, 1, 1);
    private static final LocalDate FAR_FUTURE = LocalDate.of(2100, 1, 1);

    private final PaymentRepository paymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final CashHandoverRepository cashHandoverRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    @Transactional(readOnly = true)
    public List<CashAvailableResponse> balances() {
        Map<String, BigDecimal> available = availableByKey();
        return paymentTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentType::getId))
                .map(t -> {
                    Currency currency = CurrencyMath.orBase(t.getCurrency());
                    return CashAvailableResponse.builder()
                            .paymentTypeId(t.getId())
                            .paymentTypeName(t.getName())
                            .paymentTypeIcon(t.getIcon())
                            .currency(currency)
                            .available(available.getOrDefault(key(t.getId(), currency), BigDecimal.ZERO))
                            .build();
                })
                .toList();
    }

    public BigDecimal available(Long paymentTypeId, Currency currency) {
        return availableByKey().getOrDefault(key(paymentTypeId, CurrencyMath.orBase(currency)), BigDecimal.ZERO);
    }

    private Map<String, BigDecimal> availableByKey() {
        Map<String, BigDecimal> result = new HashMap<>();
        add(result, paymentRepository.sumCashByType(BEGINNING.atStartOfDay(), FAR_FUTURE.atStartOfDay()), BigDecimal.ONE);
        add(result, supplierPaymentRepository.sumCashByType(BEGINNING.atStartOfDay(), FAR_FUTURE.atStartOfDay()),
                BigDecimal.ONE.negate());
        add(result, cashHandoverRepository.sumAcceptedByType(BEGINNING, FAR_FUTURE), BigDecimal.ONE.negate());
        add(result, expenseRepository.sumCashByType(BEGINNING, FAR_FUTURE), BigDecimal.ONE.negate());
        return result;
    }

    private static void add(Map<String, BigDecimal> target, List<Object[]> rows, BigDecimal sign) {
        for (Object[] row : rows) {
            String key = key((Long) row[0], CurrencyMath.orBase((Currency) row[1]));
            target.merge(key, toDecimal(row[2]).multiply(sign), BigDecimal::add);
        }
    }

    private static String key(Long typeId, Currency currency) {
        return typeId + ":" + currency.name();
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal d ? d : new BigDecimal(value.toString());
    }
}
