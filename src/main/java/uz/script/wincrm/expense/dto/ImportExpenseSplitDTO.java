package uz.script.wincrm.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImportExpenseSplitDTO {

    @NotNull
    @Valid
    private List<ImportExpenseSplitLineDTO> lines;
}
