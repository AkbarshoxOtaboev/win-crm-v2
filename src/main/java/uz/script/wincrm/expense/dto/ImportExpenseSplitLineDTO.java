package uz.script.wincrm.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ImportExpenseSplitLineDTO {

    @NotNull(message = "Pozitsiya tanlanishi shart")
    private Long warehouseOrderItemId;

    @NotNull(message = "Ulush summasi kerak")
    @DecimalMin(value = "0", message = "Ulush manfiy bo'lishi mumkin emas")
    private BigDecimal amount;
}
