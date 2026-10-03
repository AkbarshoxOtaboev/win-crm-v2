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
import uz.script.wincrm.currency.dto.ExchangeRateDTO;
import uz.script.wincrm.currency.response.CbuRateResponse;
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
    @Operation(summary = "Rate history for a currency (default: last 60 days)")
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
    @Operation(summary = "Rate in effect today (null if never entered)")
    public ResponseEntity<?> current(@RequestParam(defaultValue = "USD") Currency currency) {
        return ResponseEntity.ok(RestApiResponse.<ExchangeRateResponse>builder()
                .message("Current exchange rate fetched")
                .data(service.current(currency))
                .build());
    }

    @GetMapping("/cbu")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Central Bank of Uzbekistan rate as a suggestion (not saved)")
    public ResponseEntity<?> cbu(
            @RequestParam(defaultValue = "USD") Currency currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(RestApiResponse.<CbuRateResponse>builder()
                .message("CBU rate fetched")
                .data(service.cbu(currency, date))
                .build());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create or update the rate for a date")
    public ResponseEntity<?> save(@Valid @RequestBody ExchangeRateDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<ExchangeRateResponse>builder()
                .message("Exchange rate saved")
                .data(service.save(dto))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Delete a rate entry")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder()
                .message("Exchange rate deleted")
                .build());
    }
}
