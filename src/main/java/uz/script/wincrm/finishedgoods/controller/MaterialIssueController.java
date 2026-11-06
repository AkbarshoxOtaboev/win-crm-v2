package uz.script.wincrm.finishedgoods.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.finishedgoods.dto.MaterialIssueDTO;
import uz.script.wincrm.finishedgoods.response.MaterialIssueResponse;
import uz.script.wincrm.finishedgoods.service.MaterialIssueService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/material-issues")
@RequiredArgsConstructor
@Tag(name = "Material Issue (tayyor mahsulot — material chiqim)")
public class MaterialIssueController {

    private final MaterialIssueService service;

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_ISSUE_CREATE')")
    @Operation(summary = "Yangi material chiqim (DRAFT)")
    public ResponseEntity<?> create(@Valid @RequestBody MaterialIssueDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                RestApiResponse.<MaterialIssueResponse>builder()
                        .message("Material issue created")
                        .data(service.create(dto))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_ISSUE_EDIT')")
    @Operation(summary = "DRAFT material chiqimni yangilash")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody MaterialIssueDTO dto) {
        return ResponseEntity.ok(
                RestApiResponse.<MaterialIssueResponse>builder()
                        .message("Material issue updated")
                        .data(service.update(id, dto))
                        .build()
        );
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyAuthority('MATERIAL_ISSUE_CREATE', 'MATERIAL_ISSUE_EDIT')")
    @Operation(summary = "Material chiqimni tasdiqlash (ombordan OUT)")
    public ResponseEntity<?> post(@PathVariable Long id) {
        return ResponseEntity.ok(
                RestApiResponse.<MaterialIssueResponse>builder()
                        .message("Material issue posted")
                        .data(service.post(id))
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_ISSUE_VIEW')")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(
                RestApiResponse.<List<MaterialIssueResponse>>builder()
                        .message("Material issues fetched")
                        .data(service.findAll())
                        .build()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_ISSUE_VIEW')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                RestApiResponse.<MaterialIssueResponse>builder()
                        .message("Material issue fetched")
                        .data(service.findById(id))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_ISSUE_DELETE')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                RestApiResponse.<Void>builder().message("Material issue deleted").build()
        );
    }
}
