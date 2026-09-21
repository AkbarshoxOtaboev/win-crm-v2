package uz.script.wincrm.workshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.utils.RestApiResponse;
import uz.script.wincrm.workshop.dto.WorkshopDTO;
import uz.script.wincrm.workshop.response.WorkshopResponse;
import uz.script.wincrm.workshop.service.WorkshopService;

import java.util.List;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
@Tag(name = "Workshop REST API")
public class WorkshopController {

    private final WorkshopService service;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('WORKSHOP_CREATE')")
    @Operation(summary = "Create workshop")
    public ResponseEntity<?> create(@Valid @RequestBody WorkshopDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<WorkshopResponse>builder()
                        .message("Workshop successfully created")
                        .data(service.create(dto))
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSHOP_VIEW')")
    @Operation(summary = "Fetch all workshops")
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(RestApiResponse.<List<WorkshopResponse>>builder()
                .message("All workshops fetched successfully")
                .data(service.fetchAll())
                .build());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('WORKSHOP_VIEW')")
    @Operation(summary = "Fetch active workshops")
    public ResponseEntity<?> fetchActive() {
        return ResponseEntity.ok(RestApiResponse.<List<WorkshopResponse>>builder()
                .message("Active workshops fetched successfully")
                .data(service.fetchActive())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKSHOP_VIEW')")
    @Operation(summary = "Fetch workshop by id")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<WorkshopResponse>builder()
                .message("Workshop found successfully")
                .data(service.findById(id))
                .build());
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('WORKSHOP_EDIT')")
    @Operation(summary = "Update workshop")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody WorkshopDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<WorkshopResponse>builder()
                .message("Workshop successfully updated")
                .data(service.update(id, dto))
                .build());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('WORKSHOP_DELETE')")
    @Operation(summary = "Delete workshop")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder()
                .message("Workshop successfully deleted")
                .build());
    }

    @PutMapping("/{id}/change-status")
    @PreAuthorize("hasAuthority('WORKSHOP_EDIT')")
    @Operation(summary = "Toggle workshop ACTIVE/DISABLED")
    public ResponseEntity<?> changeStatus(@PathVariable Long id) {
        return ResponseEntity.ok(RestApiResponse.<WorkshopResponse>builder()
                .message("Workshop status successfully changed")
                .data(service.changeStatus(id))
                .build());
    }
}
