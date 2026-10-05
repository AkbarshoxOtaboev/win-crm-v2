package uz.script.wincrm.cash.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CashHandoverRequest {

    /** Bo'sh bo'lsa - bugun. */
    private LocalDate handoverDate;

    @Size(max = 255)
    private String comment;

    @Valid
    @NotEmpty
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private Long paymentTypeId;

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal amount;
    }
}
