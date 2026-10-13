package uz.script.wincrm.cash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.cash.response.CashAvailableResponse;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.exceptions.BadRequestException;
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
 * rahbarga topshirilgan (qabul qilingan yoki ko'rib chiqilayotgan) - kassadan qilingan xarajatlar.
 * Valyutalar bir-biriga qo'shilmaydi. Kassa hech qachon minusga tushmasligi kerak.
 */
@Service
@RequiredArgsConstructor
public class CashAvailabilityService {

    private static final LocalDate BEGINNING = LocalDate.of(2000, 1, 1);
    private static final LocalDate FAR_FUTURE = LocalDate.of(2100, 1, 1);
    private static final List<CashHandoverStatus> RESERVED =
            List.of(CashHandoverStatus.PENDING, CashHandoverStatus.ACCEPTED);

    private final PaymentRepository paymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final CashHandoverRepository cashHandoverRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    @Transactional(readOnly = true)
    public List<CashAvailableResponse> balances() {
        Map<String, BigDecimal> available = currentBalances();
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
        return currentBalances().getOrDefault(key(paymentTypeId, CurrencyMath.orBase(currency)), BigDecimal.ZERO);
    }

    /** Hozirgi qoldiqlar: key = {@link #key(Long, Currency)}. */
    public Map<String, BigDecimal> currentBalances() {
        return net(BEGINNING, FAR_FUTURE);
    }

    /** Kun boshidagi qoldiqlar - shu kundan oldingi barcha harakatlar. */
    public Map<String, BigDecimal> balancesBefore(LocalDate day) {
        return net(BEGINNING, day);
    }

    /**
     * Kassadan {@code amount} chiqarilsa minusga tushmasligini tekshiradi. {@code returned} - tahrirlanayotgan
     * hujjat avval shu kassadan olgan summa (u kassaga qaytgan deb hisoblanadi). Entity o'zgartirilishidan oldin
     * chaqirilishi kerak: aks holda so'rovdan oldingi auto-flush yangi summani qoldiqdan allaqachon ayirib qo'yadi.
     */
    public void requireEnough(PaymentType type, BigDecimal amount, BigDecimal returned, String action) {
        Currency currency = CurrencyMath.orBase(type.getCurrency());
        BigDecimal available = available(type.getId(), currency).add(returned != null ? returned : BigDecimal.ZERO);
        if (amount != null && amount.compareTo(available) > 0) {
            throw new BadRequestException("Kassada yetarli mablag' yo'q: \"" + type.getName() + "\" kassasida "
                    + plain(available.max(BigDecimal.ZERO)) + " " + currency
                    + " bor, " + action + " " + plain(amount) + " " + currency);
        }
    }

    public static String key(Long typeId, Currency currency) {
        return typeId + ":" + currency.name();
    }

    private Map<String, BigDecimal> net(LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        add(result, paymentRepository.sumCashByType(fromDate.atStartOfDay(), toDate.atStartOfDay()), BigDecimal.ONE);
        add(result, supplierPaymentRepository.sumCashByType(fromDate.atStartOfDay(), toDate.atStartOfDay()),
                BigDecimal.ONE.negate());
        add(result, cashHandoverRepository.sumByType(fromDate, toDate, RESERVED), BigDecimal.ONE.negate());
        add(result, expenseRepository.sumCashByType(fromDate, toDate), BigDecimal.ONE.negate());
        return result;
    }

    private static void add(Map<String, BigDecimal> target, List<Object[]> rows, BigDecimal sign) {
        for (Object[] row : rows) {
            String key = key((Long) row[0], CurrencyMath.orBase((Currency) row[1]));
            target.merge(key, toDecimal(row[2]).multiply(sign), BigDecimal::add);
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal d ? d : new BigDecimal(value.toString());
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
