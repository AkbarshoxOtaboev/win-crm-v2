package uz.script.wincrm.salary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.salary.dto.SalaryAdjustmentDTO;
import uz.script.wincrm.salary.dto.SalaryConfigDTO;
import uz.script.wincrm.salary.response.SalaryConfigPageResponse;
import uz.script.wincrm.salary.response.SalaryConfigResponse;
import uz.script.wincrm.salary.response.SalarySlipResponse;
import uz.script.wincrm.salary.response.SalaryTransactionPageResponse;
import uz.script.wincrm.salary.response.SalaryTransactionResponse;
import uz.script.wincrm.salary.service.SalaryConfigService;
import uz.script.wincrm.salary.service.SalarySlipService;
import uz.script.wincrm.salary.service.SalaryTransactionService;
import uz.script.wincrm.utils.PageUtils;
import uz.script.wincrm.utils.RestApiResponse;
import uz.script.wincrm.utils.response.PageResponse;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
@Tag(name = "Salary REST API", description = "Oylik ish haqi: konfiguratsiya, ledger va oylik hisob-kitob")
public class SalaryController {

    private final SalaryConfigService configService;
    private final SalaryTransactionService transactionService;
    private final SalarySlipService slipService;

    // ============================ CONFIG ============================

    @PostMapping("/configs")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_CREATE')")
    @Operation(summary = "Oylik konfiguratsiyasini yaratish",
            description = "Xodim uchun fiksa va komissiya qoidasini belgilaydi. Mavjud ochiq " +
                    "konfiguratsiya avtomatik yopiladi (effective-dating).")
    @ApiResponse(responseCode = "201",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryConfigResponse.class)))
    public ResponseEntity<?> createConfig(@Valid @RequestBody SalaryConfigDTO dto) {
        SalaryConfigResponse response = configService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<SalaryConfigResponse>builder()
                        .message("Salary config successfully created")
                        .data(response)
                        .build());
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_VIEW')")
    @Operation(summary = "Barcha oylik konfiguratsiyalari")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryConfigPageResponse.class)))
    public ResponseEntity<RestApiResponse<PageResponse<SalaryConfigResponse>>> fetchAllConfigs(
            @ParameterObject
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(RestApiResponse.<PageResponse<SalaryConfigResponse>>builder()
                .message("All salary configs fetched successfully")
                .data(PageUtils.from(configService.fetchAll(pageable)))
                .build());
    }

    @GetMapping("/configs/{id}")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_VIEW')")
    @Operation(summary = "Konfiguratsiyani id bo'yicha olish")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryConfigResponse.class)))
    public ResponseEntity<?> findConfigById(
            @Parameter(description = "Config ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<SalaryConfigResponse>builder()
                .message("Salary config found successfully")
                .data(configService.findById(id))
                .build());
    }

    @GetMapping("/configs/user/{userId}")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_VIEW')")
    @Operation(summary = "Xodimning konfiguratsiya tarixi")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryConfigPageResponse.class)))
    public ResponseEntity<RestApiResponse<PageResponse<SalaryConfigResponse>>> fetchConfigsByUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @ParameterObject
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(RestApiResponse.<PageResponse<SalaryConfigResponse>>builder()
                .message("Salary configs for user fetched successfully")
                .data(PageUtils.from(configService.fetchByUserId(userId, pageable)))
                .build());
    }

    @GetMapping("/configs/user/{userId}/current")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_VIEW')")
    @Operation(summary = "Xodimning hozir amaldagi konfiguratsiyasi")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryConfigResponse.class)))
    public ResponseEntity<?> getCurrentConfig(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(RestApiResponse.<SalaryConfigResponse>builder()
                .message("Current salary config fetched successfully")
                .data(configService.getCurrentByUserId(userId))
                .build());
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("hasAuthority('SALARY_CONFIG_DELETE')")
    @Operation(summary = "Konfiguratsiyani o'chirish (soft delete)")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<?> deleteConfig(
            @Parameter(description = "Config ID", example = "1") @PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder()
                .message("Salary config successfully deleted")
                .build());
    }

    // ========================= TRANSACTIONS =========================

    @PostMapping("/transactions/adjustment")
    @PreAuthorize("hasAuthority('SALARY_TRANSACTION_CREATE')")
    @Operation(summary = "Qo'lda tuzatish qo'shish (BONUS/DEDUCTION/ADVANCE)",
            description = "COMMISSION turlari qabul qilinmaydi - ular to'lov hodisasida avtomatik yoziladi.")
    @ApiResponse(responseCode = "201",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryTransactionResponse.class)))
    public ResponseEntity<?> addAdjustment(@Valid @RequestBody SalaryAdjustmentDTO dto) {
        SalaryTransactionResponse response = transactionService.addAdjustment(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<SalaryTransactionResponse>builder()
                        .message("Salary adjustment successfully created")
                        .data(response)
                        .build());
    }

    @GetMapping("/transactions/user/{userId}")
    @PreAuthorize("hasAuthority('SALARY_TRANSACTION_VIEW')")
    @Operation(summary = "Xodimning barcha ledger yozuvlari")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryTransactionPageResponse.class)))
    public ResponseEntity<RestApiResponse<PageResponse<SalaryTransactionResponse>>> fetchTxByUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @ParameterObject
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(RestApiResponse.<PageResponse<SalaryTransactionResponse>>builder()
                .message("Salary transactions fetched successfully")
                .data(PageUtils.from(transactionService.fetchByUserId(userId, pageable)))
                .build());
    }

    @GetMapping("/transactions/user/{userId}/period")
    @PreAuthorize("hasAuthority('SALARY_TRANSACTION_VIEW')")
    @Operation(summary = "Xodimning bitta oy bo'yicha ledger yozuvlari")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalaryTransactionPageResponse.class)))
    public ResponseEntity<RestApiResponse<PageResponse<SalaryTransactionResponse>>> fetchTxByPeriod(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "Yil", example = "2026") @RequestParam Integer year,
            @Parameter(description = "Oy (1..12)", example = "8") @RequestParam Integer month,
            @ParameterObject
            @PageableDefault(size = 20, page = 0, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(RestApiResponse.<PageResponse<SalaryTransactionResponse>>builder()
                .message("Salary transactions for period fetched successfully")
                .data(PageUtils.from(transactionService.fetchByUserAndPeriod(userId, year, month, pageable)))
                .build());
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasAuthority('SALARY_TRANSACTION_DELETE')")
    @Operation(summary = "Qo'lda tuzatishni o'chirish",
            description = "Avtomatik komissiya yozuvlarini o'chirib bo'lmaydi.")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<?> deleteTransaction(
            @Parameter(description = "Transaction ID", example = "1") @PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder()
                .message("Salary transaction successfully deleted")
                .build());
    }

    // ============================ SLIP ============================

    @GetMapping("/slip/user/{userId}")
    @PreAuthorize("hasAuthority('SALARY_SLIP_VIEW')")
    @Operation(summary = "Xodimning oylik yig'ma hisob-kitobi (payslip)",
            description = "netSalary = baseSalary + komissiya - qaytarilgan komissiya + bonus - ushlanma - avans")
    @ApiResponse(responseCode = "200",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SalarySlipResponse.class)))
    public ResponseEntity<?> getSlip(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId,
            @Parameter(description = "Yil", example = "2026") @RequestParam Integer year,
            @Parameter(description = "Oy (1..12)", example = "8") @RequestParam Integer month) {
        SalarySlipResponse response = slipService.getSlip(userId, year, month);
        return ResponseEntity.ok(RestApiResponse.<SalarySlipResponse>builder()
                .message("Salary slip fetched successfully")
                .data(response)
                .build());
    }
}
