package uz.script.wincrm.sale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.repository.SaleOrderItemRepository;
import uz.script.wincrm.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Mahsulotdagi {@code maxDiscountPercent} chegarasini sotuvchilar uchun majburlaydi.
 * Chegarani belgilay oladiganlar (GOODS_EDIT) cheklanmaydi.
 * Pozitsiya narxini katalog narxidan pasaytirish ham chegirma hisoblanadi va buyurtma
 * chegirmasi bilan birga (murakkab foiz sifatida) jami chegaradan oshmasligi kerak.
 */
@Component
@RequiredArgsConstructor
public class DiscountLimitPolicy {

    public static final String BYPASS_AUTHORITY = "GOODS_EDIT";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");

    private final SaleOrderItemRepository itemRepository;
    private final ExchangeRateService exchangeRateService;

    public boolean canBypass() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return true;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (BYPASS_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** {@code priceSelling} buyurtma valyutasida; katalog narxi shu valyutaga buyurtma kursida o'giriladi. */
    public void checkItemPrice(Goods goods, BigDecimal priceSelling, SaleOrder order) {
        if (goods == null || priceSelling == null || canBypass()) {
            return;
        }
        BigDecimal limit = limitFraction(goods);
        BigDecimal list = listPriceIn(goods, order);
        if (limit == null || list == null || list.signum() <= 0) {
            return;
        }
        BigDecimal reduction = reduction(list, priceSelling);
        if (reduction.compareTo(limit.add(TOLERANCE)) > 0) {
            BigDecimal minPrice = list.multiply(BigDecimal.ONE.subtract(limit)).setScale(2, RoundingMode.HALF_UP);
            throw new BadRequestException(String.format(
                    "«%s» uchun ruxsat etilgan maksimal chegirma %s%%. Kiritilgan narx bilan chegirma %s%% bo'ladi. Minimal narx: %s",
                    goods.getName(), percent(limit), percent(reduction),
                    CurrencyMath.format(minPrice, order != null ? order.currencyOrBase() : null)));
        }
    }

    /**
     * So'mdagi buyurtmaning kursi 1, shuning uchun xorijiy narxli mahsulot buyurtma sanasidagi kurs bilan
     * o'giriladi. Kurs kiritilmagan bo'lsa null - tekshiruv o'tkazib yuboriladi.
     */
    private BigDecimal listPriceIn(Goods goods, SaleOrder order) {
        BigDecimal list = goods.getPriceSelling();
        Currency goodsCurrency = CurrencyMath.orBase(goods.getPriceCurrency());
        if (list == null || order == null || goodsCurrency == order.currencyOrBase()) {
            return list;
        }
        BigDecimal rate = order.getExchangeRate();
        if (order.currencyOrBase().isBase()) {
            try {
                LocalDate date = order.getOrderDate() != null ? order.getOrderDate().toLocalDate() : LocalDate.now();
                rate = exchangeRateService.rateOn(goodsCurrency, date);
            } catch (BadRequestException e) {
                return null;
            }
        }
        return CurrencyMath.convert(list, goodsCurrency, order.currencyOrBase(), rate);
    }

    public void checkOrderDiscount(SaleOrder order, BigDecimal discountAmount) {
        if (order == null || order.getId() == null || discountAmount == null || discountAmount.signum() <= 0
                || canBypass()) {
            return;
        }
        BigDecimal original = order.getOriginalTotalSum();
        if (original == null || original.signum() <= 0) {
            return;
        }

        BigDecimal allowed = allowedOrderFraction(itemRepository.findAllBySaleOrderId(order.getId()));
        if (allowed == null) {
            return;
        }
        BigDecimal actual = discountAmount.divide(original, 6, RoundingMode.HALF_UP);
        if (actual.compareTo(allowed.add(TOLERANCE)) > 0) {
            BigDecimal maxAmount = original.multiply(allowed).setScale(2, RoundingMode.HALF_UP);
            throw new BadRequestException(String.format(
                    "Chegirma %s%% — bu buyurtma uchun ruxsat etilgan maksimal chegirma %s%% (%s). "
                            + "Kattaroq chegirma uchun administratorga murojaat qiling",
                    percent(actual), percent(allowed), CurrencyMath.format(maxAmount, order.currencyOrBase())));
        }
    }

    /** Pozitsiyalar qiymati bo'yicha o'rtacha qolgan chegirma ulushi; cheklangan mahsulot bo'lmasa null. */
    private BigDecimal allowedOrderFraction(List<SaleOrderItem> items) {
        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal allowedSum = BigDecimal.ZERO;
        boolean anyLimited = false;

        for (SaleOrderItem item : items) {
            if (item.getStatus() != Status.ACTIVE || item.getPriceSelling() == null || item.getCount() == null) {
                continue;
            }
            BigDecimal weight = item.getPriceSelling().multiply(item.getCount());
            if (weight.signum() <= 0) {
                continue;
            }
            Goods goods = item.getGoods();
            BigDecimal limit = limitFraction(goods);
            BigDecimal remaining;
            if (limit == null) {
                remaining = BigDecimal.ONE;
            } else {
                anyLimited = true;
                BigDecimal list = listPriceIn(goods, item.getSaleOrder());
                BigDecimal used = list != null && list.signum() > 0
                        ? reduction(list, item.getPriceSelling())
                        : BigDecimal.ZERO;
                remaining = remainingAfter(limit, used);
            }
            weightSum = weightSum.add(weight);
            allowedSum = allowedSum.add(weight.multiply(remaining));
        }

        if (!anyLimited || weightSum.signum() <= 0) {
            return null;
        }
        return allowedSum.divide(weightSum, 6, RoundingMode.HALF_UP);
    }

    /** 1 - (1 - limit) / (1 - used): pozitsiyada ishlatilgan chegirmadan keyin qolgan ulush. */
    private BigDecimal remainingAfter(BigDecimal limit, BigDecimal used) {
        if (used.compareTo(limit) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal left = BigDecimal.ONE.subtract(used);
        return BigDecimal.ONE.subtract(BigDecimal.ONE.subtract(limit).divide(left, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal reduction(BigDecimal list, BigDecimal price) {
        if (price.compareTo(list) >= 0) {
            return BigDecimal.ZERO;
        }
        return list.subtract(price).divide(list, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal limitFraction(Goods goods) {
        if (goods == null || goods.getMaxDiscountPercent() == null) {
            return null;
        }
        return goods.getMaxDiscountPercent().divide(HUNDRED, 6, RoundingMode.HALF_UP);
    }

    private String percent(BigDecimal fraction) {
        return fraction.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

}
