package uz.script.wincrm.discount.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.discount.dto.DiscountRuleRequest;
import uz.script.wincrm.discount.response.DiscountRuleResponse;
import uz.script.wincrm.discount.service.DiscountRuleService;
import uz.script.wincrm.utils.RestApiResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discount-rules")
@RequiredArgsConstructor
@Tag(name = "Discount rules REST API")
public class DiscountRuleController {

    private final DiscountRuleService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Role-based maximum order discount rules")
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(RestApiResponse.<List<DiscountRuleResponse>>builder()
                .message("Discount rules fetched")
                .data(service.fetchAll())
                .build());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Save role-based maximum order discount rules")
    public ResponseEntity<?> saveAll(@Valid @RequestBody DiscountRuleRequest request) {
        return ResponseEntity.ok(RestApiResponse.<List<DiscountRuleResponse>>builder()
                .message("Discount rules saved")
                .data(service.saveAll(request))
                .build());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Current user's maximum order discount percent (null = unlimited)")
    public ResponseEntity<?> myLimit() {
        Map<String, BigDecimal> data = new HashMap<>();
        data.put("maxDiscountPercent", service.currentUserLimit());
        return ResponseEntity.ok(RestApiResponse.<Map<String, BigDecimal>>builder()
                .message("Discount limit fetched")
                .data(data)
                .build());
    }
}
