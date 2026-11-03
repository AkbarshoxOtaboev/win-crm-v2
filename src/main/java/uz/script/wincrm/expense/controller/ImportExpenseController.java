package uz.script.wincrm.expense.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.expense.ImportExpenseKind;
import uz.script.wincrm.expense.dto.ImportExpenseDTO;
import uz.script.wincrm.expense.dto.ImportExpenseSplitDTO;
import uz.script.wincrm.expense.response.ImportExpenseItemOption;
import uz.script.wincrm.expense.response.ImportExpenseOrderOption;
import uz.script.wincrm.expense.response.ImportExpenseRecordedResponse;
import uz.script.wincrm.expense.response.ImportExpenseResponse;
import uz.script.wincrm.expense.response.ImportExpenseSplitResponse;
import uz.script.wincrm.expense.service.ImportExpenseService;
import uz.script.wincrm.utils.RestApiResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/import-expenses")
@RequiredArgsConstructor
@Tag(name = "Import Expense REST API", description = "Kirimga bog'langan boj va yo'l haqi. Oddiy xarajatlardan alohida")
public class ImportExpenseController {

    private final ImportExpenseService service;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    @Operation(summary = "Import xarajati yaratish",
            description = "Kirim buyurtma, tur (ROAD yoki DUTY) va pozitsiya. Bitta pozitsiya bo'lsa u o'zi yoziladi.")
    public ResponseEntity<?> create(@Valid @RequestBody ImportExpenseDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<ImportExpenseResponse>builder()
                        .message("Import xarajati saqlandi")
                        .data(service.create(dto))
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    @Operation(summary = "Import xarajatlari ro'yxati")
    public ResponseEntity<?> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long warehouseOrderId,
            @RequestParam(required = false) ImportExpenseKind kind
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<ImportExpenseResponse>>builder()
                .message("Import xarajatlari")
                .data(service.search(startDate, endDate, warehouseOrderId, kind))
                .build());
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    @Operation(summary = "Import xarajati uchun kirim buyurtmalar")
    public ResponseEntity<?> orders() {
        return ResponseEntity.ok(RestApiResponse.<List<ImportExpenseOrderOption>>builder()
                .message("Kirim buyurtmalar")
                .data(service.orders())
                .build());
    }

    @GetMapping("/orders/{orderId}/items")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    @Operation(summary = "Tanlangan kirimning pozitsiyalari")
    public ResponseEntity<?> items(@PathVariable Long orderId) {
        return ResponseEntity.ok(RestApiResponse.<List<ImportExpenseItemOption>>builder()
                .message("Pozitsiyalar")
                .data(service.items(orderId))
                .build());
    }

    @GetMapping("/recorded")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    @Operation(summary = "Pozitsiya va tur bo'yicha avval yozilgan summa",
            description = "Tahrirda excludeId berilsa, shu yozuvning o'zi qo'shilmaydi.")
    public ResponseEntity<?> recorded(
            @RequestParam Long warehouseOrderItemId,
            @RequestParam ImportExpenseKind kind,
            @RequestParam(required = false) Long excludeId
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<ImportExpenseRecordedResponse>>builder()
                .message("Avval yozilgan summa")
                .data(service.recorded(warehouseOrderItemId, kind, excludeId))
                .build());
    }

    @GetMapping("/{id}/split")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    @Operation(summary = "Kirim pozitsiyalariga taqsimot")
    public ResponseEntity<?> split(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<ImportExpenseSplitResponse>builder()
                .message("Taqsimot")
                .data(service.split(id))
                .build());
    }

    @PutMapping("/{id}/split")
    @PreAuthorize("hasAuthority('EXPENSE_EDIT')")
    @Operation(summary = "Umumiy summani pozitsiyalarga taqsimlash",
            description = "Kassadan pul chiqmaydi. Ulushlar yig'indisi umumiy summadan oshmasin.")
    public ResponseEntity<?> saveSplit(@PathVariable Long id, @Valid @RequestBody ImportExpenseSplitDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<ImportExpenseSplitResponse>builder()
                .message("Taqsimot saqlandi")
                .data(service.saveSplit(id, dto))
                .build());
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_EDIT')")
    @Operation(summary = "Import xarajatini tahrirlash")
    public ResponseEntity<?> update(
            @Parameter(description = "Import xarajat ID") @PathVariable Long id,
            @Valid @RequestBody ImportExpenseDTO dto
    ) {
        return ResponseEntity.ok(RestApiResponse.<ImportExpenseResponse>builder()
                .message("Import xarajati yangilandi")
                .data(service.update(id, dto))
                .build());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_DELETE')")
    @Operation(summary = "Import xarajatini o'chirish",
            description = "Pul kassaga qaytadi. Kirim buyurtma o'zgarmaydi.")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder()
                .message("Import xarajati o'chirildi")
                .build());
    }
}
