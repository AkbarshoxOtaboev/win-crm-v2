package uz.script.wincrm.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.script.wincrm.utils.PageUtils;
import uz.script.wincrm.utils.RestApiResponse;
import uz.script.wincrm.utils.response.PageResponse;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit log management rest api")
public class AuditLogController {
    private final AuditLogService service;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sahifalangan audit loglar (filter: username, fromDate, toDate)")
    @ApiResponse(responseCode = "200", content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AuditPageResponse.class)
    ))
    public ResponseEntity<RestApiResponse<PageResponse<AuditResponse>>> allLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @ParameterObject
            @PageableDefault(size = 50, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                RestApiResponse.<PageResponse<AuditResponse>>builder()
                        .message("Audit logs fetched successfully")
                        .data(PageUtils.from(service.findAll(username, fromDate, toDate, pageable)))
                        .build()
        );
    }
}
