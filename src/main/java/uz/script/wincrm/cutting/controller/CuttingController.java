package uz.script.wincrm.cutting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.dto.CuttingOptimizeRequest;
import uz.script.wincrm.cutting.response.CuttingOptimizeResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.service.CuttingOptimizeService;
import uz.script.wincrm.cutting.service.CuttingService;
import uz.script.wincrm.utils.RestApiResponse;

@RestController
@RequestMapping("/api/cutting")
@RequiredArgsConstructor
@Tag(name = "Cutting REST API")
public class CuttingController {

    private final CuttingService service;
    private final CuttingOptimizeService optimizeService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT', 'PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Preview how many sheets the pieces need and what is left over (nothing is saved)")
    public ResponseEntity<?> calculate(@Valid @RequestBody CuttingCalculateRequest request) {
        return ResponseEntity.ok(RestApiResponse.<CuttingResultResponse>builder()
                .message("Cutting calculated")
                .data(service.calculate(request))
                .build());
    }

    @GetMapping("/optimize/{saleOrderId}")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Kesish optimizatsiyasi: oyna qatorlari va foydali qoldiqlar")
    public ResponseEntity<?> loadOptimize(@PathVariable Long saleOrderId) {
        return ResponseEntity.ok(RestApiResponse.<CuttingOptimizeResponse>builder()
                .message("Cutting optimization")
                .data(optimizeService.load(saleOrderId))
                .build());
    }

    @PostMapping("/optimize/{saleOrderId}")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Optimizatsiyani tasdiqlash: list va qoldiq taqsimoti, qoldiqlarni yozish")
    public ResponseEntity<?> confirmOptimize(
            @PathVariable Long saleOrderId,
            @Valid @RequestBody CuttingOptimizeRequest request
    ) {
        return ResponseEntity.ok(RestApiResponse.<CuttingOptimizeResponse>builder()
                .message("Cutting optimization saved")
                .data(optimizeService.confirm(saleOrderId, request))
                .build());
    }
}
