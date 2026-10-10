package uz.script.wincrm.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cash.repository.CashHandoverRepository;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.ReportFx;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.dashboard.responses.*;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.expense.Expense;
import uz.script.wincrm.expense.ExpenseCategory;
import uz.script.wincrm.expense.repository.ExpenseRepository;
import uz.script.wincrm.payment.Payment;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.repository.PaymentRepository;
import uz.script.wincrm.payment.repository.PaymentTypeRepository;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.suppliers.repository.SupplierPaymentRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final int TOP_LIMIT = 10;
    private static final LocalDateTime BEGINNING = LocalDate.of(2000, 1, 1).atStartOfDay();

    private final SaleOrderItemRepository repository;
    private final SaleOrderRepository saleOrderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final CashHandoverRepository cashHandoverRepository;
    private final ExpenseRepository expenseRepository;
    private final ExchangeRateService exchangeRateService;

    /** Mahsulot/guruh bo'yicha yig'ilgan miqdor va ko'rsatish valyutasidagi summa. */
    private record Totals(Long id, String name, BigDecimal count, BigDecimal amount) {
    }

    @Override
    public List<TopGoodsResponse> fetchTopGoodsByQuantity(LocalDate startDate, LocalDate endDate, Currency display) {
        log.info("Fetch TOP {} goods by quantity: {} - {} ({})", TOP_LIMIT, startDate, endDate, display);
        return goodsTotals(startDate, endDate, display, row -> (Long) row[0], row -> (String) row[1]).stream()
                .sorted(Comparator.comparing(Totals::count).reversed())
                .limit(TOP_LIMIT)
                .map(t -> new TopGoodsResponse(t.id(), t.name(), t.count(), t.amount()))
                .toList();
    }

    @Override
    public List<TopGoodsResponse> fetchTopGoodsByAmount(LocalDate startDate, LocalDate endDate, Currency display) {
        log.info("Fetch TOP {} goods by amount: {} - {} ({})", TOP_LIMIT, startDate, endDate, display);
        return goodsTotals(startDate, endDate, display, row -> (Long) row[0], row -> (String) row[1]).stream()
                .sorted(Comparator.comparing(Totals::amount).reversed())
                .limit(TOP_LIMIT)
                .map(t -> new TopGoodsResponse(t.id(), t.name(), t.count(), t.amount()))
                .toList();
    }

    @Override
    public List<GoodsGroupSummaryResponse> fetchGoodsGroupSummary(LocalDate startDate, LocalDate endDate, Currency display) {
        log.info("Fetch goods group summary: {} - {} ({})", startDate, endDate, display);
        return goodsTotals(startDate, endDate, display, row -> (Long) row[2], row -> (String) row[3]).stream()
                .filter(t -> t.id() != null)
                .sorted(Comparator.comparing(Totals::amount).reversed())
                .map(t -> new GoodsGroupSummaryResponse(t.id(), t.name(), t.count(), t.amount()))
                .toList();
    }

    @Override
    public List<TopSellerResponse> fetchTopSellers(LocalDate startDate, LocalDate endDate, Currency display) {
        log.info("Fetch TOP {} sellers: {} - {} ({})", TOP_LIMIT, startDate, endDate, display);

        LocalDateTime[] range = toDateTimeRange(startDate, endDate);
        ReportFx fx = ReportFx.of(exchangeRateService, display);

        Map<Long, TopSellerResponse> byUser = new LinkedHashMap<>();
        for (Object[] row : saleOrderRepository.findSellerSalesRows(range[0], range[1])) {
            Long userId = (Long) row[0];
            BigDecimal amount = fx.convert(toDecimal(row[6]), (Currency) row[2], toDecimal(row[3]), toDate(row[4]));
            TopSellerResponse seller = byUser.computeIfAbsent(userId,
                    id -> new TopSellerResponse(id, (String) row[1], 0L, BigDecimal.ZERO));
            seller.setOrderCount(seller.getOrderCount() + (Long) row[5]);
            seller.setTotalAmount(seller.getTotalAmount().add(amount));
        }
        return byUser.values().stream()
                .sorted(Comparator.comparing(TopSellerResponse::getTotalAmount).reversed())
                .limit(TOP_LIMIT)
                .toList();
    }

    @Override
    public List<PaymentTypeSummaryResponse> fetchPaymentSummaryByType(LocalDateTime fromDate, LocalDateTime toDate) {
        log.info("Fetch payment summary by type: {} - {}", fromDate, toDate);

        validateRange(fromDate, toDate);

        List<Payment> payments = paymentRepository.findAllInRangeWithType(fromDate, toDate);

        Map<PaymentType, List<Payment>> byType = payments.stream()
                .filter(p -> p.getPaymentType() != null)
                .collect(Collectors.groupingBy(Payment::getPaymentType));

        return byType.entrySet().stream()
                .map(entry -> PaymentTypeSummaryResponse.builder()
                        .paymentTypeId(entry.getKey().getId())
                        .paymentTypeName(entry.getKey().getName())
                        .currency(CurrencyMath.orBase(entry.getKey().getCurrency()))
                        .totalAmount(entry.getValue().stream()
                                .map(Payment::getPaymentAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .paymentCount(entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(PaymentTypeSummaryResponse::getCurrency)
                        .thenComparing(PaymentTypeSummaryResponse::getTotalAmount, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<DailyPaymentSummaryResponse> fetchDailyPayments(LocalDateTime fromDate, LocalDateTime toDate, Currency display) {
        log.info("Fetch daily payments: {} - {} ({})", fromDate, toDate, display);

        validateRange(fromDate, toDate);
        ReportFx fx = ReportFx.of(exchangeRateService, display);

        List<Payment> payments = paymentRepository.findAllInRangeWithType(fromDate, toDate);

        Map<LocalDate, List<Payment>> byDate = payments.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentDate().toLocalDate()));

        List<DailyPaymentSummaryResponse> result = new ArrayList<>();

        LocalDate cursor = fromDate.toLocalDate();
        LocalDate lastDay = toDate.toLocalDate();

        // Har bir kun uchun (to'lov bo'lmagan kunlar ham 0 summa bilan) qator hosil qilamiz
        while (!cursor.isAfter(lastDay)) {
            List<Payment> dayPayments = byDate.getOrDefault(cursor, List.of());

            Map<PaymentType, BigDecimal> amountByType = dayPayments.stream()
                    .filter(p -> p.getPaymentType() != null)
                    .collect(Collectors.groupingBy(
                            Payment::getPaymentType,
                            Collectors.reducing(BigDecimal.ZERO, Payment::getPaymentAmount, BigDecimal::add)
                    ));

            List<DailyPaymentSummaryResponse.PaymentTypeAmount> byType = amountByType.entrySet().stream()
                    .map(entry -> DailyPaymentSummaryResponse.PaymentTypeAmount.builder()
                            .paymentTypeId(entry.getKey().getId())
                            .paymentTypeName(entry.getKey().getName())
                            .amount(entry.getValue())
                            .currency(CurrencyMath.orBase(entry.getKey().getCurrency()))
                            .build())
                    .toList();

            BigDecimal dayTotal = dayPayments.stream()
                    .map(p -> fx.convert(p.getPaymentAmount(), p.getCurrency(), cashRate(p), p.getPaymentDate().toLocalDate()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(DailyPaymentSummaryResponse.builder()
                    .date(cursor)
                    .totalAmount(dayTotal)
                    .byType(byType)
                    .build());

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    @Override
    public List<DailyExpenseReportResponse> fetchExpenseReport(LocalDate fromDate, LocalDate toDate, Currency display) {
        log.info("Fetch expense report: {} - {} ({})", fromDate, toDate, display);

        ReportFx fx = ReportFx.of(exchangeRateService, display);
        Map<Long, DailyExpenseReportResponse> byCategory = new LinkedHashMap<>();
        for (Expense e : expenseRepository.findExpenseReportByExpenesCategory(fromDate, toDate)) {
            ExpenseCategory category = e.getCategory();
            DailyExpenseReportResponse row = byCategory.computeIfAbsent(category.getId(),
                    id -> new DailyExpenseReportResponse(id, category.getName(), BigDecimal.ZERO));
            row.setTotalAmount(row.getTotalAmount().add(fx.convert(e.getAmount(), Currency.BASE, null, e.getExpenseDate())));
        }
        return new ArrayList<>(byCategory.values());
    }

    @Override
    public List<CashBalanceResponse> fetchCashBalances(LocalDate fromDate, LocalDate toDate) {
        log.info("Fetch cash balances: {} - {}", fromDate, toDate);

        LocalDateTime[] range = toDateTimeRange(fromDate, toDate);
        LocalDateTime start = range[0];
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();

        Map<String, BigDecimal> inBefore = cashSums(paymentRepository.sumCashByType(BEGINNING, start));
        Map<String, BigDecimal> outBefore = cashSums(supplierPaymentRepository.sumCashByType(BEGINNING, start));
        Map<String, BigDecimal> in = cashSums(paymentRepository.sumCashByType(start, end));
        Map<String, BigDecimal> out = cashSums(supplierPaymentRepository.sumCashByType(start, end));
        Map<String, BigDecimal> handedBefore = cashSums(cashHandoverRepository.sumAcceptedByType(BEGINNING.toLocalDate(), fromDate));
        Map<String, BigDecimal> handed = cashSums(cashHandoverRepository.sumAcceptedByType(fromDate, toDate.plusDays(1)));

        Set<String> keys = new LinkedHashSet<>();
        List<PaymentType> types = paymentTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(PaymentType::getId))
                .toList();
        types.forEach(t -> keys.add(cashKey(t.getId(), CurrencyMath.orBase(t.getCurrency()))));
        keys.addAll(inBefore.keySet());
        keys.addAll(outBefore.keySet());
        keys.addAll(in.keySet());
        keys.addAll(out.keySet());
        keys.addAll(handedBefore.keySet());
        keys.addAll(handed.keySet());

        Map<Long, PaymentType> typeById = types.stream().collect(Collectors.toMap(PaymentType::getId, Function.identity()));

        List<CashBalanceResponse> result = new ArrayList<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            Long typeId = Long.valueOf(parts[0]);
            PaymentType type = typeById.get(typeId);
            if (type == null) {
                continue;
            }
            BigDecimal opening = inBefore.getOrDefault(key, BigDecimal.ZERO)
                    .subtract(outBefore.getOrDefault(key, BigDecimal.ZERO))
                    .subtract(handedBefore.getOrDefault(key, BigDecimal.ZERO));
            BigDecimal incoming = in.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal outgoing = out.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal handedOver = handed.getOrDefault(key, BigDecimal.ZERO);
            result.add(CashBalanceResponse.builder()
                    .paymentTypeId(typeId)
                    .paymentTypeName(type.getName())
                    .currency(Currency.valueOf(parts[1]))
                    .opening(opening)
                    .incoming(incoming)
                    .outgoing(outgoing)
                    .handedOver(handedOver)
                    .closing(opening.add(incoming).subtract(outgoing).subtract(handedOver))
                    .build());
        }
        return result;
    }

    @Override
    public FxDifferenceResponse fetchFxDifference(LocalDate fromDate, LocalDate toDate) {
        log.info("Fetch FX difference: {} - {}", fromDate, toDate);

        LocalDateTime[] range = toDateTimeRange(fromDate, toDate);
        ReportFx fx = ReportFx.of(exchangeRateService, Currency.BASE);

        List<FxDifferenceResponse.Row> rows = new ArrayList<>();
        long unallocated = 0;
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;

        for (Payment p : paymentRepository.findForeignDebtPaymentsInRange(range[0], range[1])) {
            SaleOrder order = p.getSaleOrder();
            if (order == null) {
                unallocated++;
                continue;
            }
            Currency debtCurrency = p.getDebtCurrency();
            BigDecimal applied = p.appliedOrPaid();
            BigDecimal orderRate = order.getExchangeRate();
            if (applied == null || orderRate == null || order.currencyOrBase() != debtCurrency) {
                continue;
            }
            BigDecimal paymentRate = p.getCurrency() != debtCurrency && p.getExchangeRate() != null
                    ? p.getExchangeRate()
                    : fx.rate(debtCurrency, p.getPaymentDate().toLocalDate());
            BigDecimal orderBase = applied.multiply(orderRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paymentBase = applied.multiply(paymentRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal difference = paymentBase.subtract(orderBase);
            if (difference.signum() > 0) {
                gain = gain.add(difference);
            } else {
                loss = loss.add(difference);
            }
            rows.add(FxDifferenceResponse.Row.builder()
                    .paymentId(p.getId())
                    .paymentDate(p.getPaymentDate())
                    .saleOrderId(order.getId())
                    .orderDate(order.getOrderDate())
                    .clientId(p.getClient() != null ? p.getClient().getId() : null)
                    .clientFullName(p.getClient() != null ? p.getClient().getFullName() : null)
                    .debtCurrency(debtCurrency)
                    .appliedAmount(applied)
                    .paidAmount(p.getPaymentAmount())
                    .paidCurrency(p.getCurrency())
                    .orderRate(orderRate)
                    .paymentRate(paymentRate)
                    .orderBase(orderBase)
                    .paymentBase(paymentBase)
                    .difference(difference)
                    .build());
        }

        return FxDifferenceResponse.builder()
                .totalDifference(gain.add(loss))
                .totalGain(gain)
                .totalLoss(loss)
                .unallocatedCount(unallocated)
                .rows(rows)
                .build();
    }

    /** Foyda hisobotida jamlanadigan qiymatlar (ko'rsatish valyutasida). */
    private static final class ProfitAcc {
        String name;
        Type type;
        String unit;
        long orders;
        BigDecimal count = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        void add(BigDecimal revenue, BigDecimal cost) {
            this.revenue = this.revenue.add(revenue);
            this.cost = this.cost.add(cost);
        }

        BigDecimal profit() {
            return revenue.subtract(cost);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitReportResponse fetchProfitReport(LocalDate fromDate, LocalDate toDate, Currency display) {
        log.info("Fetch profit report: {} - {} ({})", fromDate, toDate, display);

        LocalDateTime[] range = toDateTimeRange(fromDate, toDate);
        ReportFx fx = ReportFx.of(exchangeRateService, display);

        ProfitAcc total = new ProfitAcc();
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal delivery = BigDecimal.ZERO;
        long itemless = 0;
        Map<LocalDate, ProfitAcc> byDay = new TreeMap<>();
        Map<Long, ProfitAcc> byGoods = new HashMap<>();
        Map<Long, ProfitAcc> bySeller = new HashMap<>();

        List<SaleOrder> orders = saleOrderRepository.findForProfitReport(range[0], range[1]);
        for (SaleOrder order : orders) {
            LocalDate day = order.getOrderDate().toLocalDate();
            Currency currency = order.currencyOrBase();
            BigDecimal rate = order.getExchangeRate();

            BigDecimal revenue = fx.convert(order.totalSumWithoutDelivery(), currency, rate, day);
            discount = discount.add(fx.convert(order.getDiscountAmount(), currency, rate, day));
            delivery = delivery.add(fx.convert(order.deliveryFeeOrZero(), currency, rate, day));

            List<SaleOrderItem> items = order.getSaleOrderItems() == null ? List.of()
                    : order.getSaleOrderItems().stream()
                    .filter(i -> i.getStatus() != Status.DELETED && i.getGoods() != null && i.getCount() != null)
                    .toList();
            BigDecimal itemsTotal = items.stream()
                    .map(i -> i.getCount().multiply(nz(i.getPriceSelling())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal orderCost = BigDecimal.ZERO;
            if (itemsTotal.signum() > 0) {
                for (SaleOrderItem item : items) {
                    BigDecimal share = item.getCount().multiply(nz(item.getPriceSelling()))
                            .divide(itemsTotal, 10, RoundingMode.HALF_UP);
                    BigDecimal itemRevenue = revenue.multiply(share).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal itemCost = costIn(fx, item.getCount().multiply(nz(item.getPriceCost())), order, day);
                    orderCost = orderCost.add(itemCost);

                    Goods goods = item.getGoods();
                    ProfitAcc g = byGoods.computeIfAbsent(goods.getId(), id -> new ProfitAcc());
                    g.name = goods.getName();
                    g.type = goods.getType();
                    g.unit = goods.getUnitType() != null ? goods.getUnitType().getName() : null;
                    g.count = g.count.add(item.getCount());
                    g.add(itemRevenue, itemCost);
                }
            } else {
                itemless++;
            }

            total.orders++;
            total.add(revenue, orderCost);
            byDay.computeIfAbsent(day, d -> new ProfitAcc()).add(revenue, orderCost);
            if (order.getUser() != null) {
                ProfitAcc s = bySeller.computeIfAbsent(order.getUser().getId(), id -> new ProfitAcc());
                s.name = order.getUser().getFullName();
                s.orders++;
                s.add(revenue, orderCost);
            }
        }

        List<ProfitReportResponse.Day> days = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            ProfitAcc acc = byDay.getOrDefault(d, new ProfitAcc());
            days.add(ProfitReportResponse.Day.builder()
                    .date(d)
                    .revenue(acc.revenue)
                    .cost(acc.cost)
                    .profit(acc.profit())
                    .build());
        }

        List<ProfitReportResponse.GoodsRow> goods = byGoods.entrySet().stream()
                .map(e -> ProfitReportResponse.GoodsRow.builder()
                        .goodsId(e.getKey())
                        .goodsName(e.getValue().name)
                        .goodsType(e.getValue().type)
                        .unitName(e.getValue().unit)
                        .count(e.getValue().count)
                        .revenue(e.getValue().revenue)
                        .cost(e.getValue().cost)
                        .profit(e.getValue().profit())
                        .marginPercent(margin(e.getValue()))
                        .build())
                .sorted(Comparator.comparing(ProfitReportResponse.GoodsRow::getProfit).reversed())
                .toList();

        List<ProfitReportResponse.SellerRow> sellers = bySeller.entrySet().stream()
                .map(e -> ProfitReportResponse.SellerRow.builder()
                        .userId(e.getKey())
                        .fullName(e.getValue().name)
                        .orderCount(e.getValue().orders)
                        .revenue(e.getValue().revenue)
                        .cost(e.getValue().cost)
                        .profit(e.getValue().profit())
                        .marginPercent(margin(e.getValue()))
                        .build())
                .sorted(Comparator.comparing(ProfitReportResponse.SellerRow::getProfit).reversed())
                .toList();

        return ProfitReportResponse.builder()
                .currency(fx.display())
                .revenue(total.revenue)
                .cost(total.cost)
                .profit(total.profit())
                .marginPercent(margin(total))
                .discount(discount)
                .deliveryFee(delivery)
                .orderCount(total.orders)
                .itemlessOrderCount(itemless)
                .days(days)
                .goods(goods)
                .sellers(sellers)
                .build();
    }

    /**
     * Tannarx so'mda saqlanadi. Buyurtma valyutasi ko'rsatish valyutasi bilan bir xil bo'lsa, buyurtma kursi
     * ishlatiladi - tushum ham shu kursda, foyda kurs farqidan buzilmaydi.
     */
    private static BigDecimal costIn(ReportFx fx, BigDecimal baseCost, SaleOrder order, LocalDate day) {
        if (fx.display().isBase()) {
            return baseCost.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = order.getExchangeRate();
        if (order.currencyOrBase() == fx.display() && rate != null && rate.signum() > 0) {
            return baseCost.divide(rate, 2, RoundingMode.HALF_UP);
        }
        return fx.convert(baseCost, Currency.BASE, null, day);
    }

    private static BigDecimal margin(ProfitAcc acc) {
        if (acc.revenue.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return acc.profit().multiply(BigDecimal.valueOf(100)).divide(acc.revenue, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<Totals> goodsTotals(LocalDate startDate, LocalDate endDate, Currency display,
                                     Function<Object[], Long> idOf, Function<Object[], String> nameOf) {
        LocalDateTime[] range = toDateTimeRange(startDate, endDate);
        ReportFx fx = ReportFx.of(exchangeRateService, display);

        Map<Long, Totals> totals = new LinkedHashMap<>();
        for (Object[] row : repository.findGoodsSalesRows(range[0], range[1])) {
            Long id = idOf.apply(row);
            BigDecimal count = toDecimal(row[7]);
            BigDecimal amount = fx.convert(toDecimal(row[8]), (Currency) row[4], toDecimal(row[5]), toDate(row[6]));
            totals.merge(id, new Totals(id, nameOf.apply(row), count, amount),
                    (a, b) -> new Totals(a.id(), a.name(), a.count().add(b.count()), a.amount().add(b.amount())));
        }
        return new ArrayList<>(totals.values());
    }

    /** Kassa valyutasini so'mga o'girish uchun to'lovda qotirilgan kurs (faqat kassa va qarz valyutasi farq qilsa). */
    private static BigDecimal cashRate(Payment p) {
        return p.getCurrency() != p.getDebtCurrency() ? p.getExchangeRate() : null;
    }

    private static Map<String, BigDecimal> cashSums(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.merge(cashKey((Long) row[0], CurrencyMath.orBase((Currency) row[1])), toDecimal(row[2]), BigDecimal::add);
        }
        return map;
    }

    private static String cashKey(Long typeId, Currency currency) {
        return typeId + ":" + currency.name();
    }

    private static LocalDate toDate(Object value) {
        return switch (value) {
            case LocalDate d -> d;
            case LocalDateTime dt -> dt.toLocalDate();
            case java.sql.Date d -> d.toLocalDate();
            case null, default -> null;
        };
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal d) {
            return d;
        }
        return new BigDecimal(value.toString());
    }

    /**
     * LocalDate oralig'ini to'liq LocalDateTime oralig'iga o'giradi:
     * startDate 00:00:00'dan, endDate 23:59:59.999999999'gacha.
     */
    private LocalDateTime[] toDateTimeRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate va endDate majburiy");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate endDate'dan katta bo'lishi mumkin emas");
        }

        return new LocalDateTime[]{startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)};
    }

    private void validateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate va toDate majburiy");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate toDate'dan katta bo'lishi mumkin emas");
        }
    }
}
