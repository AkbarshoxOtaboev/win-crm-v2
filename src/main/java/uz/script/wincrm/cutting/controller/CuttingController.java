package uz.script.wincrm.cutting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.dto.CuttingOptimizeRequest;
import uz.script.wincrm.cutting.dto.CuttingSuggestRequest;
import uz.script.wincrm.cutting.dto.ManualLayoutRequest;
import uz.script.wincrm.cutting.dto.RemnantDeleteRequest;
import uz.script.wincrm.cutting.response.CuttingOptimizeResponse;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.response.ManualLayoutResponse;
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

    @GetMapping("/remnants")
    @PreAuthorize("hasAnyAuthority('STOCK_VIEW', 'SALE_ORDER_VIEW', 'GOODS_VIEW')")
    @Operation(summary = "Ombordagi foydali qoldiqlar, o'lcham bo'yicha")
    public ResponseEntity<?> remnants() {
        return ResponseEntity.ok(RestApiResponse.<java.util.List<CuttingOptimizeResponse.RemnantStock>>builder()
                .message("Glass remnants")
                .data(optimizeService.listRemnants())
                .build());
    }

    @DeleteMapping("/remnants/{id}")
    @PreAuthorize("hasAnyAuthority('STOCK_DELETE', 'STOCK_EDIT', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Bitta foydali qoldiqni o'chirish")
    public ResponseEntity<Void> deleteRemnant(@PathVariable Long id) {
        optimizeService.deleteRemnants(java.util.List.of(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/remnants/delete")
    @PreAuthorize("hasAnyAuthority('STOCK_DELETE', 'STOCK_EDIT', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Tanlangan foydali qoldiqlarni o'chirish")
    public ResponseEntity<Void> deleteRemnants(@Valid @RequestBody RemnantDeleteRequest request) {
        optimizeService.deleteRemnants(request.getIds());
        return ResponseEntity.noContent().build();
    }

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

    @PostMapping("/optimize/{saleOrderId}/suggest")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Algoritm list va qoldiqni o'zi tanlaydi. Hech narsa saqlanmaydi.")
    public ResponseEntity<?> suggest(
            @PathVariable Long saleOrderId,
            @RequestBody(required = false) CuttingSuggestRequest request
    ) {
        return ResponseEntity.ok(RestApiResponse.<CuttingOptimizeResponse>builder()
                .message("Cutting suggestion")
                .data(optimizeService.suggest(saleOrderId, request))
                .build());
    }

    @GetMapping("/optimize/{saleOrderId}/manual")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT', 'PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Qo'lda tahrirlangan kesish kartasi")
    public ResponseEntity<?> manualLayout(@PathVariable Long saleOrderId) {
        return ResponseEntity.ok(RestApiResponse.<ManualLayoutResponse>builder()
                .message("Manual cutting layout")
                .data(optimizeService.manualLayout(saleOrderId))
                .build());
    }

    @PutMapping("/optimize/{saleOrderId}/manual")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Qo'lda tahrirlangan kesish kartasini saqlash (oldingisi tarixda qoladi)")
    public ResponseEntity<?> saveManualLayout(
            @PathVariable Long saleOrderId,
            @Valid @RequestBody ManualLayoutRequest request
    ) {
        return ResponseEntity.ok(RestApiResponse.<ManualLayoutResponse>builder()
                .message("Manual cutting layout saved")
                .data(optimizeService.saveManualLayout(saleOrderId, request.getLayout()))
                .build());
    }

    @PostMapping("/optimize/{saleOrderId}/manual/restore")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_CREATE', 'SALE_ORDER_EDIT')")
    @Operation(summary = "Oldingi saqlangan kesish kartasini qaytarish")
    public ResponseEntity<?> restoreManualLayout(@PathVariable Long saleOrderId) {
        return ResponseEntity.ok(RestApiResponse.<ManualLayoutResponse>builder()
                .message("Manual cutting layout restored")
                .data(optimizeService.restoreManualLayout(saleOrderId))
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
