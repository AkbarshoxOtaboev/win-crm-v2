package uz.script.wincrm.discount.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DiscountRuleRequest {

    @Valid
    @NotNull
    private List<Item> rules;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Long roleId;

        /** null - cheklovsiz. */
        @DecimalMin("0")
        @DecimalMax("100")
        private BigDecimal maxDiscountPercent;
    }
}
