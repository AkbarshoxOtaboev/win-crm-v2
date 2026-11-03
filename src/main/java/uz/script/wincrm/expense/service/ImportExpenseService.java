package uz.script.wincrm.expense.service;

import uz.script.wincrm.expense.ImportExpenseKind;
import uz.script.wincrm.expense.dto.ImportExpenseDTO;
import uz.script.wincrm.expense.dto.ImportExpenseSplitDTO;
import uz.script.wincrm.expense.response.ImportExpenseItemOption;
import uz.script.wincrm.expense.response.ImportExpenseOrderOption;
import uz.script.wincrm.expense.response.ImportExpenseRecordedResponse;
import uz.script.wincrm.expense.response.ImportExpenseResponse;
import uz.script.wincrm.expense.response.ImportExpenseSplitResponse;

import java.time.LocalDate;
import java.util.List;

public interface ImportExpenseService {

    ImportExpenseResponse create(ImportExpenseDTO dto);

    ImportExpenseResponse update(Long id, ImportExpenseDTO dto);

    void delete(Long id);

    List<ImportExpenseResponse> search(LocalDate startDate, LocalDate endDate, Long warehouseOrderId, ImportExpenseKind kind);

    List<ImportExpenseRecordedResponse> recorded(Long warehouseOrderItemId, ImportExpenseKind kind, Long excludeId);

    List<ImportExpenseOrderOption> orders();

    List<ImportExpenseItemOption> items(Long warehouseOrderId);

    ImportExpenseSplitResponse split(Long id);

    ImportExpenseSplitResponse saveSplit(Long id, ImportExpenseSplitDTO dto);
}
