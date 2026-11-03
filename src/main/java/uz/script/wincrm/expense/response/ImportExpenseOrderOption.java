package uz.script.wincrm.expense.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Import xarajati formasidagi kirim tanlovi. */
@Getter
@Builder
public class ImportExpenseOrderOption {

    private Long id;
    private String supplierName;
    private LocalDateTime arrivalDate;
}
