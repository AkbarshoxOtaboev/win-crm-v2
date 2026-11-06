package uz.script.wincrm.sale.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum SalesOrderStatus {
    NEW,
    CONFIRMED,
    PROCESSING,
    READY,
    /** Transport bo'limiga yuborilgan: qabul, yo'lga chiqish, sotuv menejeri tasdig'i kutilmoqda. */
    IN_DELIVERY,
    DELIVERED,
    /** Ish bajarildi: COMPLETED ga faqat buyurtma bo'yicha qarz qolmaganda o'tadi. */
    WORK_DONE,
    COMPLETED,
    CANCELLED;

    /** Asosiy jarayon tartibi (CANCELLED alohida). Orqaga qaytarish shu tartib bo'yicha. */
    public static final List<SalesOrderStatus> MAIN_FLOW = List.of(
            NEW, CONFIRMED, PROCESSING, READY, IN_DELIVERY, DELIVERED, WORK_DONE, COMPLETED
    );

    private static final Map<SalesOrderStatus, Set<SalesOrderStatus>> TRANSITIONS = new EnumMap<>(SalesOrderStatus.class);

    static {
        TRANSITIONS.put(NEW, EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(PROCESSING, READY, CANCELLED));
        TRANSITIONS.put(PROCESSING, EnumSet.of(READY, CANCELLED));
        // READY -> DELIVERED: mijoz o'zi olib ketganda (transportsiz)
        TRANSITIONS.put(READY, EnumSet.of(IN_DELIVERY, DELIVERED, CANCELLED));
        TRANSITIONS.put(IN_DELIVERY, EnumSet.of(DELIVERED, CANCELLED));
        TRANSITIONS.put(DELIVERED, EnumSet.of(WORK_DONE, CANCELLED));
        TRANSITIONS.put(WORK_DONE, EnumSet.of(COMPLETED, CANCELLED));
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(SalesOrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(SalesOrderStatus.class));
    }

    public boolean canTransitionTo(SalesOrderStatus target) {
        if (target == null || isFinal()) {
            return false;
        }
        if (TRANSITIONS.getOrDefault(this, EnumSet.noneOf(SalesOrderStatus.class)).contains(target)) {
            return true;
        }
        return isReverseTo(target);
    }

    /** Oldinga (yoki CANCELLED) o'tish — side-effectlar shu yo'lda ishlaydi. */
    public boolean isForwardTo(SalesOrderStatus target) {
        if (target == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(SalesOrderStatus.class)).contains(target);
    }

    /** MAIN_FLOW ichida orqaga (oldingi bosqichga) qaytarish. */
    public boolean isReverseTo(SalesOrderStatus target) {
        if (target == null || isFinal() || target == CANCELLED) {
            return false;
        }
        int from = MAIN_FLOW.indexOf(this);
        int to = MAIN_FLOW.indexOf(target);
        return from > 0 && to >= 0 && to < from;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
