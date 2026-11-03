package uz.script.wincrm.workshop;

import org.junit.jupiter.api.Test;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.workshop.enums.WorkshopPayBasis;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkshopPayCalculatorTest {

    @Test
    void measuresCuttingPackageAndEdgeFromSheetSize() {
        Goods glass = Goods.builder().type(Type.WINDOW).build();
        SaleOrderItem line = SaleOrderItem.builder()
                .goods(glass)
                .width(new BigDecimal("50"))
                .height(new BigDecimal("70"))
                .count(new BigDecimal("7"))
                .build();

        assertEquals(new BigDecimal("7.0000"),
                WorkshopPayCalculator.measure(List.of(line), WorkshopPayBasis.SQUARE_METER));
        assertEquals(new BigDecimal("48.0000"),
                WorkshopPayCalculator.measure(List.of(line), WorkshopPayBasis.PERIMETER));
        assertEquals(new BigDecimal("48.0000"),
                WorkshopPayCalculator.measure(List.of(line), WorkshopPayBasis.LENGTH));
    }
}
