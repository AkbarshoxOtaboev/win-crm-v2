package uz.script.wincrm.production.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.production.dto.CompleteProductionDTO;
import uz.script.wincrm.production.dto.RedirectProductionDTO;
import uz.script.wincrm.production.dto.SendToProductionDTO;
import uz.script.wincrm.production.response.ProductionEventResponse;
import uz.script.wincrm.production.response.ProductionOrderResponse;
import uz.script.wincrm.production.service.ProductionOrderService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/production-orders")
@RequiredArgsConstructor
@Tag(name = "Production Order REST API")
public class ProductionOrderController {

    private final ProductionOrderService service;

    @PostMapping("/send-to-production")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_CREATE')")
    @Operation(summary = "Send sale order to production with first workshop")
    public ResponseEntity<?> sendToProduction(@Valid @RequestBody SendToProductionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<ProductionOrderResponse>builder()
                        .message("Sent to production successfully")
                        .data(service.sendToProduction(dto))
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "List production orders")
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(RestApiResponse.<List<ProductionOrderResponse>>builder()
                .message("Production orders fetched")
                .data(service.fetchAll())
                .build());
    }

    @GetMapping("/board")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Workshop board — pending/active assignments")
    public ResponseEntity<?> board(@RequestParam Long workshopId) {
        return ResponseEntity.ok(RestApiResponse.<List<ProductionOrderResponse>>builder()
                .message("Workshop board fetched")
                .data(service.board(workshopId))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Get production order")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<ProductionOrderResponse>builder()
                .message("Production order found")
                .data(service.findById(id))
                .build());
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Production chronology")
    public ResponseEntity<?> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<List<ProductionEventResponse>>builder()
                .message("Timeline fetched")
                .data(service.timeline(id))
                .build());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_EDIT')")
    @Operation(summary = "Start current workshop assignment")
    public ResponseEntity<?> start(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<ProductionOrderResponse>builder()
                .message("Started")
                .data(service.start(id))
                .build());
    }

    @PostMapping("/{id}/redirect")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_EDIT')")
    @Operation(summary = "Finish current workshop and redirect to next")
    public ResponseEntity<?> redirect(
            @PathVariable Long id,
            @Valid @RequestBody RedirectProductionDTO dto
    ) {
        return ResponseEntity.ok(RestApiResponse.<ProductionOrderResponse>builder()
                .message("Redirected")
                .data(service.redirect(id, dto))
                .build());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PRODUCTION_ORDER_EDIT')")
    @Operation(summary = "Finish current workshop and mark production done")
    public ResponseEntity<?> complete(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteProductionDTO dto
    ) {
        return ResponseEntity.ok(RestApiResponse.<ProductionOrderResponse>builder()
                .message("Completed")
                .data(service.complete(id, dto != null ? dto : new CompleteProductionDTO()))
                .build());
    }
}
