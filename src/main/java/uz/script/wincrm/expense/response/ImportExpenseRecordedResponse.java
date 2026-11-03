package uz.script.wincrm.expense.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import uz.script.wincrm.currency.Currency;

import java.math.BigDecimal;

/** Shu pozitsiya va tur bo'yicha avval yozilgan summa, valyuta bo'yicha. */
@Getter
@Builder
@Schema(name = "Import Expense Recorded")
public class ImportExpenseRecordedResponse {

    private Currency currency;
    private BigDecimal amount;
}
