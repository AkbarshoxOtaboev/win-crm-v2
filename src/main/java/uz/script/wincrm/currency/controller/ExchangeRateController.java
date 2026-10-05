package uz.script.wincrm.currency.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.dto.CompanyFxRateRequest;
import uz.script.wincrm.currency.response.CompanyFxRateResponse;
import uz.script.wincrm.currency.response.ExchangeRateResponse;
import uz.script.wincrm.currency.service.CompanyFxRateService;
import uz.script.wincrm.currency.service.ExchangeRateService;
import uz.script.wincrm.utils.RestApiResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange rates REST API")
public class ExchangeRateController {

    private final ExchangeRateService service;
    private final CompanyFxRateService companyFxRateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Central Bank rate history for a currency (default: last 60 days), newest first")
    public ResponseEntity<?> history(
            @RequestParam(defaultValue = "USD") Currency currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<ExchangeRateResponse>>builder()
                .message("Exchange rates fetched")
                .data(service.history(currency, from, to))
                .build());
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rate in effect on a date (default today) - the rate documents of that date use; null if unknown")
    public ResponseEntity<?> current(
            @RequestParam(defaultValue = "USD") Currency currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(RestApiResponse.<ExchangeRateResponse>builder()
                .message("Current exchange rate fetched")
                .data(service.on(currency, date))
                .build());
    }

    @GetMapping("/company")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Company buy/sell rates per foreign currency, with today's Central Bank rate")
    public ResponseEntity<?> companyRates() {
        return ResponseEntity.ok(RestApiResponse.<List<CompanyFxRateResponse>>builder()
                .message("Company rates fetched")
                .data(companyFxRateService.fetchAll())
                .build());
    }

    @PutMapping("/company/{currency}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR')")
    @Operation(summary = "Set company buy/sell rate for a currency (SUPER_ADMIN / ADMIN / DIRECTOR)")
    public ResponseEntity<?> saveCompanyRate(
            @PathVariable Currency currency,
            @Valid @RequestBody CompanyFxRateRequest request
    ) {
        return ResponseEntity.ok(RestApiResponse.<CompanyFxRateResponse>builder()
                .message("Company rate saved")
                .data(companyFxRateService.save(currency, request))
                .build());
    }

    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fetch the latest Central Bank rate now (also runs hourly)")
    public ResponseEntity<?> sync(@RequestParam(defaultValue = "USD") Currency currency) {
        return ResponseEntity.ok(RestApiResponse.<ExchangeRateResponse>builder()
                .message("Exchange rate synced")
                .data(service.syncLatest(currency))
                .build());
    }
}
