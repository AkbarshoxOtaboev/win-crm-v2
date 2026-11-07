package uz.script.wincrm.sale;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleOrderNumbersTest {

    @Test
    void formatUsesTwoDigitYear() {
        assertEquals("1/26", SaleOrderNumbers.format(1, 2026));
        assertEquals("668/26", SaleOrderNumbers.format(668, 2026));
        assertEquals("1/27", SaleOrderNumbers.format(1, 2027));
    }

    @Test
    void newYearStartsAtOneEvenIfPreviousYearIsHigh() {
        SaleOrder lastYear = order(1L, LocalDateTime.of(2026, 12, 31, 10, 0));
        SaleOrder nextYear = order(2L, LocalDateTime.of(2027, 1, 2, 9, 0));
        List<SaleOrder> rows = new ArrayList<>(List.of(nextYear, lastYear));
        SaleOrderNumbers.assignMissing(rows, year -> year == 2026 ? 667 : 0);
        assertEquals(668, lastYear.getOrderSeq());
        assertEquals(2026, lastYear.getOrderYear());
        assertEquals("668/26", lastYear.orderNumber());
        assertEquals(1, nextYear.getOrderSeq());
        assertEquals("1/27", nextYear.orderNumber());
    }

    @Test
    void sameYearFollowsDateThenId() {
        SaleOrder later = order(9L, LocalDateTime.of(2026, 3, 2, 0, 0));
        SaleOrder earlier = order(4L, LocalDateTime.of(2026, 1, 1, 0, 0));
        List<SaleOrder> rows = new ArrayList<>(List.of(later, earlier));
        SaleOrderNumbers.assignMissing(rows, year -> 0);
        assertEquals(Map.of(4L, 1, 9L, 2), Map.of(earlier.getId(), earlier.getOrderSeq(), later.getId(), later.getOrderSeq()));
    }

    private static SaleOrder order(long id, LocalDateTime date) {
        SaleOrder order = new SaleOrder();
        order.setId(id);
        order.setOrderDate(date);
        return order;
    }
}
