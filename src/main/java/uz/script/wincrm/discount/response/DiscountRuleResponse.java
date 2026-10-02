package uz.script.wincrm.discount.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DiscountRuleResponse {
    private Long roleId;
    private String roleName;
    /** null - cheklovsiz. */
    private BigDecimal maxDiscountPercent;
    /** Bu rol uchun chegara qo'llanmaydi (SUPER_ADMIN). */
    private boolean locked;
}
