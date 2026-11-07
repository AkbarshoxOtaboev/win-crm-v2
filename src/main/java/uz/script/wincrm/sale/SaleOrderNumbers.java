package uz.script.wincrm.sale;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Buyurtma raqami yil bo'yicha: 2026-yildagi birinchi buyurtma 1/26,
 * keyingi yil 1/27 dan boshlanadi.
 */
public final class SaleOrderNumbers {

    private SaleOrderNumbers() {
    }

    public static String format(Integer seq, Integer year) {
        if (seq == null || year == null) {
            return null;
        }
        return seq + "/" + String.format("%02d", Math.floorMod(year, 100));
    }

    public static int yearOf(LocalDateTime orderDate, LocalDateTime createdAt) {
        LocalDateTime when = orderDate != null ? orderDate : createdAt;
        return (when != null ? when : LocalDateTime.now()).getYear();
    }

    /** Raqamsiz buyurtmalarga yil ichida sana, keyin id bo'yicha ketma-ket raqam beradi. */
    public static void assignMissing(List<SaleOrder> missing, IntUnaryOperator maxSeqOfYear) {
        missing.sort(Comparator
                .comparing((SaleOrder order) -> order.getOrderDate() != null ? order.getOrderDate() : order.getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SaleOrder::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        Map<Integer, Integer> next = new HashMap<>();
        for (SaleOrder order : missing) {
            int year = yearOf(order.getOrderDate(), order.getCreatedAt());
            int seq = next.compute(year, (key, current) -> (current == null ? maxSeqOfYear.applyAsInt(key) : current) + 1);
            order.setOrderYear(year);
            order.setOrderSeq(seq);
        }
    }
}
