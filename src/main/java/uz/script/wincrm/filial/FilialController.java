package uz.script.wincrm.filial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/filials")
@RequiredArgsConstructor
@Tag(name = "Filial REST API")
public class FilialController {

    private final FilialService service;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create filial")
    public ResponseEntity<?> create(@Valid @RequestBody FilialDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                RestApiResponse.<FilialResponse>builder()
                        .message("Filial yaratildi")
                        .data(service.create(dto))
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List filials")
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(
                RestApiResponse.<List<FilialResponse>>builder()
                        .message("Filiallar")
                        .data(service.fetchAll())
                        .build()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get filial by id")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                RestApiResponse.<FilialResponse>builder()
                        .message("Filial")
                        .data(service.findById(id))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update filial")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody FilialDTO dto) {
        return ResponseEntity.ok(
                RestApiResponse.<FilialResponse>builder()
                        .message("Filial yangilandi")
                        .data(service.update(id, dto))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete filial")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                RestApiResponse.<Void>builder()
                        .message("Filial o'chirildi")
                        .build()
        );
    }

    @PutMapping("/{id}/director")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign filial director")
    public ResponseEntity<?> assignDirector(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body
    ) {
        Long directorId = body.get("directorId");
        return ResponseEntity.ok(
                RestApiResponse.<FilialResponse>builder()
                        .message("Direktor tayinlandi")
                        .data(service.assignDirector(id, directorId))
                        .build()
        );
    }
}
