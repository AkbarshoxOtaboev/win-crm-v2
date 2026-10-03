package uz.script.wincrm.currency.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.currency.response.ExchangeRateResponse;
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
