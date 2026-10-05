package uz.script.wincrm.cash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.cash.dto.CashHandoverDecisionRequest;
import uz.script.wincrm.cash.dto.CashHandoverRequest;
import uz.script.wincrm.cash.response.CashHandoverResponse;
import uz.script.wincrm.cash.response.CashHandoverSummaryResponse;
import uz.script.wincrm.cash.service.CashHandoverService;
import uz.script.wincrm.utils.RestApiResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cash-handovers")
@RequiredArgsConstructor
@Tag(name = "Cash Handover REST API")
public class CashHandoverController {

    private final CashHandoverService service;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('CASH_HANDOVER_VIEW')")
    @Operation(summary = "Employee's daily takings per cashbox: received, paid out, handed over, remaining")
    public ResponseEntity<?> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long userId
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<CashHandoverSummaryResponse>>builder()
                .message("Cash handover summary fetched")
                .data(service.summary(date, userId))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CASH_HANDOVER_VIEW')")
    @Operation(summary = "Cash handovers (reviewers see everyone, others only their own)")
    public ResponseEntity<?> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) CashHandoverStatus status,
            @RequestParam(required = false) Long userId
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<CashHandoverResponse>>builder()
                .message("Cash handovers fetched")
                .data(service.list(fromDate, toDate, status, userId))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CASH_HANDOVER_CREATE')")
    @Operation(summary = "Hand over the day's takings (one row per cashbox)")
    public ResponseEntity<?> create(@Valid @RequestBody CashHandoverRequest request) {
        return ResponseEntity.ok(RestApiResponse.<List<CashHandoverResponse>>builder()
                .message("Cash handed over")
                .data(service.create(request))
                .build());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('CASH_HANDOVER_EDIT')")
    @Operation(summary = "Accept a pending cash handover")
    public ResponseEntity<?> accept(@PathVariable Long id, @Valid @RequestBody(required = false) CashHandoverDecisionRequest request) {
        return ResponseEntity.ok(RestApiResponse.<CashHandoverResponse>builder()
                .message("Cash handover accepted")
                .data(service.accept(id, request != null ? request.getComment() : null))
                .build());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('CASH_HANDOVER_EDIT')")
    @Operation(summary = "Reject a pending cash handover")
    public ResponseEntity<?> reject(@PathVariable Long id, @Valid @RequestBody(required = false) CashHandoverDecisionRequest request) {
        return ResponseEntity.ok(RestApiResponse.<CashHandoverResponse>builder()
                .message("Cash handover rejected")
                .data(service.reject(id, request != null ? request.getComment() : null))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASH_HANDOVER_CREATE')")
    @Operation(summary = "Cancel own pending cash handover")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.ok(RestApiResponse.builder().message("Cash handover cancelled").build());
    }
}
