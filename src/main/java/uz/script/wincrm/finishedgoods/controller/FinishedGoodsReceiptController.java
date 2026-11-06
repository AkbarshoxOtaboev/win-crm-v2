package uz.script.wincrm.finishedgoods.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.finishedgoods.dto.FinishedGoodsReceiptDTO;
import uz.script.wincrm.finishedgoods.response.FinishedGoodsReceiptResponse;
import uz.script.wincrm.finishedgoods.service.FinishedGoodsReceiptService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/finished-goods-receipts")
@RequiredArgsConstructor
@Tag(name = "Finished Goods Receipt (tayyor mahsulot prixodi)")
public class FinishedGoodsReceiptController {

    private final FinishedGoodsReceiptService service;

    @PostMapping
    @PreAuthorize("hasAuthority('FINISHED_GOODS_RECEIPT_CREATE')")
    @Operation(summary = "Yangi tayyor mahsulot prixodi (DRAFT)")
    public ResponseEntity<?> create(@Valid @RequestBody FinishedGoodsReceiptDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                RestApiResponse.<FinishedGoodsReceiptResponse>builder()
                        .message("Finished goods receipt created")
                        .data(service.create(dto))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINISHED_GOODS_RECEIPT_EDIT')")
    @Operation(summary = "DRAFT prixodni yangilash")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody FinishedGoodsReceiptDTO dto) {
        return ResponseEntity.ok(
                RestApiResponse.<FinishedGoodsReceiptResponse>builder()
                        .message("Finished goods receipt updated")
                        .data(service.update(id, dto))
                        .build()
        );
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyAuthority('FINISHED_GOODS_RECEIPT_CREATE', 'FINISHED_GOODS_RECEIPT_EDIT')")
    @Operation(summary = "Prixodni tasdiqlash (omborga IN)")
    public ResponseEntity<?> post(@PathVariable Long id) {
        return ResponseEntity.ok(
                RestApiResponse.<FinishedGoodsReceiptResponse>builder()
                        .message("Finished goods receipt posted")
                        .data(service.post(id))
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINISHED_GOODS_RECEIPT_VIEW')")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(
                RestApiResponse.<List<FinishedGoodsReceiptResponse>>builder()
                        .message("Finished goods receipts fetched")
                        .data(service.findAll())
                        .build()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINISHED_GOODS_RECEIPT_VIEW')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                RestApiResponse.<FinishedGoodsReceiptResponse>builder()
                        .message("Finished goods receipt fetched")
                        .data(service.findById(id))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINISHED_GOODS_RECEIPT_DELETE')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                RestApiResponse.<Void>builder().message("Finished goods receipt deleted").build()
        );
    }
}
