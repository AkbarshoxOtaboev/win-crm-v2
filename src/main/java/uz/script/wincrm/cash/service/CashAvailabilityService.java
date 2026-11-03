package uz.script.wincrm.cash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.cash.response.CashAvailableResponse;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.expense.repository.ImportExpenseRepository;
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
 * Ikki kassa qoldig'i (filial filter bilan):
 * <ul>
 *   <li>OPERATIONAL = mijoz kirimi − topshirish(PENDING+ACCEPTED) − operatsion chiqimlar</li>
 *   <li>VAULT = ACCEPTED topshirish − rahbar chiqimlari</li>
 * </ul>
 * PENDING topshirilgan pul hech qayerdan sarflanmaydi. Kassa minusga tushmasligi kerak.
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
    private final ImportExpenseRepository importExpenseRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    @Transactional(readOnly = true)
    public List<CashAvailableResponse> balances() {
        Map<String, BigDecimal> operational = operationalBalances();
        Map<String, BigDecimal> vault = vaultBalances();
        return paymentTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentType::getId))
                .map(t -> {
                    Currency currency = CurrencyMath.orBase(t.getCurrency());
                    String k = key(t.getId(), currency);
                    BigDecimal ops = operational.getOrDefault(k, BigDecimal.ZERO);
                    BigDecimal vlt = vault.getOrDefault(k, BigDecimal.ZERO);
                    return CashAvailableResponse.builder()
                            .paymentTypeId(t.getId())
                            .paymentTypeName(t.getName())
                            .paymentTypeIcon(t.getIcon())
                            .currency(currency)
                            .available(ops)
                            .operationalAvailable(ops)
                            .vaultAvailable(vlt)
                            .build();
                })
                .toList();
    }

    public BigDecimal available(Long paymentTypeId, Currency currency) {
        return available(paymentTypeId, currency, CashSource.OPERATIONAL);
    }

    public BigDecimal available(Long paymentTypeId, Currency currency, CashSource source) {
        CashSource src = source != null ? source : CashSource.OPERATIONAL;
        Map<String, BigDecimal> map = src == CashSource.VAULT ? vaultBalances() : operationalBalances();
        return map.getOrDefault(key(paymentTypeId, CurrencyMath.orBase(currency)), BigDecimal.ZERO);
    }

    /** Hozirgi operatsion qoldiqlar (eski API bilan mos). */
    public Map<String, BigDecimal> currentBalances() {
        return operationalBalances();
    }

    public Map<String, BigDecimal> operationalBalances() {
        return operationalNet(BEGINNING, FAR_FUTURE);
    }

    public Map<String, BigDecimal> vaultBalances() {
        return vaultNet(BEGINNING, FAR_FUTURE);
    }

    /** Kun boshidagi operatsion qoldiqlar — shu kundan oldingi harakatlar. */
    public Map<String, BigDecimal> balancesBefore(LocalDate day) {
        return operationalNet(BEGINNING, day);
    }

    /**
     * Kassadan {@code amount} chiqarilsa minusga tushmasligini tekshiradi. {@code returned} — tahrirlanayotgan
     * hujjat avval shu kassadan olgan summa. Entity o'zgartirilishidan oldin chaqirilishi kerak.
     */
    public void requireEnough(PaymentType type, BigDecimal amount, BigDecimal returned, String action) {
        requireEnough(type, amount, returned, action, CashSource.OPERATIONAL);
    }

    public void requireEnough(PaymentType type, BigDecimal amount, BigDecimal returned, String action, CashSource source) {
        CashSource src = source != null ? source : CashSource.OPERATIONAL;
        Currency currency = CurrencyMath.orBase(type.getCurrency());
        BigDecimal available = available(type.getId(), currency, src)
                .add(returned != null ? returned : BigDecimal.ZERO);
        if (amount != null && amount.compareTo(available) > 0) {
            String pool = src == CashSource.VAULT ? "Rahbar kassasi" : "Operatsion kassa";
            throw new BadRequestException(pool + "da yetarli mablag' yo'q: \"" + type.getName() + "\" da "
                    + plain(available.max(BigDecimal.ZERO)) + " " + currency
                    + " bor, " + action + " " + plain(amount) + " " + currency);
        }
    }

    public static String key(Long typeId, Currency currency) {
        return typeId + ":" + currency.name();
    }

    private Map<String, BigDecimal> operationalNet(LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        add(result, paymentRepository.sumCashByType(fromDate.atStartOfDay(), toDate.atStartOfDay()), BigDecimal.ONE);
        add(result, supplierPaymentRepository.sumCashByType(fromDate.atStartOfDay(), toDate.atStartOfDay(),
                CashSource.OPERATIONAL), BigDecimal.ONE.negate());
        add(result, cashHandoverRepository.sumByType(fromDate, toDate, RESERVED), BigDecimal.ONE.negate());
        add(result, expenseRepository.sumCashByType(fromDate, toDate, CashSource.OPERATIONAL), BigDecimal.ONE.negate());
        add(result, importExpenseRepository.sumCashByType(fromDate, toDate, CashSource.OPERATIONAL), BigDecimal.ONE.negate());
        return result;
    }

    private Map<String, BigDecimal> vaultNet(LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> result = new HashMap<>();
        add(result, cashHandoverRepository.sumAcceptedByType(fromDate, toDate), BigDecimal.ONE);
        add(result, supplierPaymentRepository.sumCashByType(fromDate.atStartOfDay(), toDate.atStartOfDay(),
                CashSource.VAULT), BigDecimal.ONE.negate());
        add(result, expenseRepository.sumCashByType(fromDate, toDate, CashSource.VAULT), BigDecimal.ONE.negate());
        add(result, importExpenseRepository.sumCashByType(fromDate, toDate, CashSource.VAULT), BigDecimal.ONE.negate());
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
