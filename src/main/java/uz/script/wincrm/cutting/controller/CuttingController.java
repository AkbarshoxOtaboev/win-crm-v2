package uz.script.wincrm.cutting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.cutting.service.CuttingService;
import uz.script.wincrm.utils.RestApiResponse;

@RestController
@RequestMapping("/api/cutting")
@RequiredArgsConstructor
@Tag(name = "Cutting REST API")
public class CuttingController {

    private final CuttingService service;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('SALE_ORDER_VIEW', 'SALE_ORDER_CREATE', 'SALE_ORDER_EDIT', 'PRODUCTION_ORDER_VIEW')")
    @Operation(summary = "Preview how many sheets the pieces need and what is left over (nothing is saved)")
    public ResponseEntity<?> calculate(@Valid @RequestBody CuttingCalculateRequest request) {
        return ResponseEntity.ok(RestApiResponse.<CuttingResultResponse>builder()
                .message("Cutting calculated")
                .data(service.calculate(request))
                .build());
    }
}
