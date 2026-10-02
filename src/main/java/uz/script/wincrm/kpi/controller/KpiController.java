package uz.script.wincrm.kpi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.kpi.dto.KpiRateDTO;
import uz.script.wincrm.kpi.response.KpiEntryResponse;
import uz.script.wincrm.kpi.response.KpiRateResponse;
import uz.script.wincrm.kpi.response.KpiSummaryResponse;
import uz.script.wincrm.kpi.service.KpiService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
@Tag(name = "KPI REST API")
public class KpiController {

    private final KpiService service;

    @GetMapping("/rates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Employees with their KPI percent")
    public ResponseEntity<?> rates() {
        return ResponseEntity.ok(RestApiResponse.<List<KpiRateResponse>>builder()
                .message("KPI rates fetched")
                .data(service.fetchRates())
                .build());
    }

    @PutMapping("/rates/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Set employee KPI percent (empty or 0 removes it)")
    public ResponseEntity<?> setRate(@PathVariable Long userId, @Valid @RequestBody KpiRateDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<KpiRateResponse>builder()
                .message("KPI rate saved")
                .data(service.setRate(userId, dto.getPercent()))
                .build());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    @Operation(summary = "Employees KPI for a month and all-time totals")
    public ResponseEntity<?> summary(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(RestApiResponse.<List<KpiSummaryResponse>>builder()
                .message("KPI summary fetched")
                .data(service.summary(year, month))
                .build());
    }

    @GetMapping("/users/{userId}/entries")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    @Operation(summary = "Employee KPI reconciliation (month omitted = all periods)")
    public ResponseEntity<?> userEntries(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<KpiEntryResponse>>builder()
                .message("KPI entries fetched")
                .data(service.userEntries(userId, year, month))
                .build());
    }

    @GetMapping("/workshops/summary")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    @Operation(summary = "Workshops earnings for a month and all-time totals")
    public ResponseEntity<?> workshopSummary(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(RestApiResponse.<List<KpiSummaryResponse>>builder()
                .message("Workshop summary fetched")
                .data(service.workshopSummary(year, month))
                .build());
    }

    @GetMapping("/workshops/{workshopId}/entries")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    @Operation(summary = "Workshop earnings reconciliation (month omitted = all periods)")
    public ResponseEntity<?> workshopEntries(
            @PathVariable Long workshopId,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<KpiEntryResponse>>builder()
                .message("Workshop entries fetched")
                .data(service.workshopEntries(workshopId, year, month))
                .build());
    }
}
