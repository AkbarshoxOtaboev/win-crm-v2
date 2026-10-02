package uz.script.wincrm.discount.service;

import uz.script.wincrm.discount.dto.DiscountRuleRequest;
import uz.script.wincrm.discount.response.DiscountRuleResponse;
import uz.script.wincrm.sale.SaleOrder;

import java.math.BigDecimal;
import java.util.List;

public interface DiscountRuleService {

    List<DiscountRuleResponse> fetchAll();

    List<DiscountRuleResponse> saveAll(DiscountRuleRequest request);

    /** Joriy foydalanuvchi rollari bo'yicha maksimal buyurtma chegirmasi (%); null - cheklovsiz. */
    BigDecimal currentUserLimit();

    void checkOrderDiscount(SaleOrder order, BigDecimal discountAmount);
}
