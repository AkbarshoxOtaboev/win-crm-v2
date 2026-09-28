package uz.script.wincrm.transport.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.script.wincrm.transport.dto.DeliveryCrewDTO;
import uz.script.wincrm.transport.dto.SalaryDecisionDTO;
import uz.script.wincrm.transport.dto.TransportDriverDTO;
import uz.script.wincrm.transport.dto.TransportSettingDTO;
import uz.script.wincrm.transport.dto.TransportWorkerDTO;
import uz.script.wincrm.transport.enums.DeliveryStatus;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;
import uz.script.wincrm.transport.response.DeliveryResponse;
import uz.script.wincrm.transport.response.TransportDriverResponse;
import uz.script.wincrm.transport.response.TransportWorkerResponse;
import uz.script.wincrm.transport.response.WorkerSalaryResponse;
import uz.script.wincrm.transport.response.WorkerSalarySummaryResponse;
import uz.script.wincrm.transport.service.TransportDeliveryService;
import uz.script.wincrm.transport.service.TransportDriverService;
import uz.script.wincrm.transport.service.TransportSalaryService;
import uz.script.wincrm.transport.service.TransportWorkerService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
@Tag(name = "Transport REST API")
public class TransportController {

    private final TransportDeliveryService deliveryService;
    private final TransportDriverService driverService;
    private final TransportWorkerService workerService;
    private final TransportSalaryService salaryService;

    // ---------- Dashboard ----------

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_VIEW')")
    @Operation(summary = "Transport dashboard")
    public ResponseEntity<?> dashboard() {
        return ok("Transport dashboard fetched", deliveryService.dashboard());
    }

    // ---------- Deliveries ----------

    @GetMapping("/deliveries")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_VIEW')")
    @Operation(summary = "List deliveries, optionally filtered by status")
    public ResponseEntity<?> deliveries(@RequestParam(required = false) List<DeliveryStatus> statuses) {
        return ResponseEntity.ok(RestApiResponse.<List<DeliveryResponse>>builder()
                .message("Deliveries fetched")
                .data(deliveryService.fetchAll(statuses))
                .build());
    }

    @GetMapping("/deliveries/{id}")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_VIEW')")
    @Operation(summary = "Get delivery")
    public ResponseEntity<?> delivery(@PathVariable Long id) {
        return ok("Delivery found", deliveryService.findById(id));
    }

    @GetMapping("/deliveries/by-sale-order/{saleOrderId}")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_VIEW')")
    @Operation(summary = "Get delivery of a sale order (data is null if not sent to transport)")
    public ResponseEntity<?> deliveryBySaleOrder(@PathVariable Long saleOrderId) {
        return ok("Delivery fetched", deliveryService.findBySaleOrderId(saleOrderId));
    }

    @PostMapping("/deliveries/{id}/accept")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_EDIT')")
    @Operation(summary = "Accept delivery and assign driver + workers")
    public ResponseEntity<?> accept(@PathVariable Long id, @Valid @RequestBody DeliveryCrewDTO dto) {
        return ok("Delivery accepted", deliveryService.accept(id, dto));
    }

    @PutMapping("/deliveries/{id}/crew")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_EDIT')")
    @Operation(summary = "Change driver / workers before confirmation")
    public ResponseEntity<?> updateCrew(@PathVariable Long id, @Valid @RequestBody DeliveryCrewDTO dto) {
        return ok("Delivery crew updated", deliveryService.updateCrew(id, dto));
    }

    @PostMapping("/deliveries/{id}/depart")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_EDIT')")
    @Operation(summary = "Mark delivery as departed")
    public ResponseEntity<?> depart(@PathVariable Long id) {
        return ok("Delivery departed", deliveryService.depart(id));
    }

    @PostMapping("/deliveries/{id}/arrive")
    @PreAuthorize("hasAuthority('TRANSPORT_DELIVERY_EDIT')")
    @Operation(summary = "Mark delivery as delivered to client (awaits sales manager confirmation)")
    public ResponseEntity<?> arrive(@PathVariable Long id) {
        return ok("Delivery arrived", deliveryService.arrive(id));
    }

    // ---------- Drivers ----------

    @GetMapping("/drivers")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_VIEW')")
    @Operation(summary = "List drivers")
    public ResponseEntity<?> drivers() {
        return ResponseEntity.ok(RestApiResponse.<List<TransportDriverResponse>>builder()
                .message("Drivers fetched")
                .data(driverService.fetchAll())
                .build());
    }

    @GetMapping("/drivers/active")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_VIEW')")
    @Operation(summary = "List active drivers")
    public ResponseEntity<?> activeDrivers() {
        return ResponseEntity.ok(RestApiResponse.<List<TransportDriverResponse>>builder()
                .message("Active drivers fetched")
                .data(driverService.fetchActive())
                .build());
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_CREATE')")
    @Operation(summary = "Create driver")
    public ResponseEntity<?> createDriver(@Valid @RequestBody TransportDriverDTO dto) {
        return created("Driver created", driverService.create(dto));
    }

    @PutMapping("/drivers/{id}")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_EDIT')")
    @Operation(summary = "Update driver")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @Valid @RequestBody TransportDriverDTO dto) {
        return ok("Driver updated", driverService.update(id, dto));
    }

    @PutMapping("/drivers/{id}/change-status")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_EDIT')")
    @Operation(summary = "Toggle driver ACTIVE/DISABLED")
    public ResponseEntity<?> changeDriverStatus(@PathVariable Long id) {
        return ok("Driver status changed", driverService.changeStatus(id));
    }

    @DeleteMapping("/drivers/{id}")
    @PreAuthorize("hasAuthority('TRANSPORT_DRIVER_DELETE')")
    @Operation(summary = "Delete driver")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        driverService.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder().message("Driver deleted").build());
    }

    // ---------- Workers ----------

    @GetMapping("/workers")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_VIEW')")
    @Operation(summary = "List workers")
    public ResponseEntity<?> workers() {
        return ResponseEntity.ok(RestApiResponse.<List<TransportWorkerResponse>>builder()
                .message("Workers fetched")
                .data(workerService.fetchAll())
                .build());
    }

    @GetMapping("/workers/active")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_VIEW')")
    @Operation(summary = "List active workers")
    public ResponseEntity<?> activeWorkers() {
        return ResponseEntity.ok(RestApiResponse.<List<TransportWorkerResponse>>builder()
                .message("Active workers fetched")
                .data(workerService.fetchActive())
                .build());
    }

    @PostMapping("/workers")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_CREATE')")
    @Operation(summary = "Create worker")
    public ResponseEntity<?> createWorker(@Valid @RequestBody TransportWorkerDTO dto) {
        return created("Worker created", workerService.create(dto));
    }

    @PutMapping("/workers/{id}")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_EDIT')")
    @Operation(summary = "Update worker")
    public ResponseEntity<?> updateWorker(@PathVariable Long id, @Valid @RequestBody TransportWorkerDTO dto) {
        return ok("Worker updated", workerService.update(id, dto));
    }

    @PutMapping("/workers/{id}/change-status")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_EDIT')")
    @Operation(summary = "Toggle worker ACTIVE/DISABLED")
    public ResponseEntity<?> changeWorkerStatus(@PathVariable Long id) {
        return ok("Worker status changed", workerService.changeStatus(id));
    }

    @DeleteMapping("/workers/{id}")
    @PreAuthorize("hasAuthority('TRANSPORT_WORKER_DELETE')")
    @Operation(summary = "Delete worker")
    public ResponseEntity<?> deleteWorker(@PathVariable Long id) {
        workerService.delete(id);
        return ResponseEntity.ok(RestApiResponse.<Void>builder().message("Worker deleted").build());
    }

    // ---------- Worker salaries ----------

    @GetMapping("/salaries")
    @PreAuthorize("hasAuthority('TRANSPORT_SALARY_VIEW')")
    @Operation(summary = "Worker salary entries for a month (defaults to current month)")
    public ResponseEntity<?> salaries(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) WorkerSalaryStatus status,
            @RequestParam(required = false) Long workerId
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<WorkerSalaryResponse>>builder()
                .message("Worker salaries fetched")
                .data(salaryService.fetch(year, month, status, workerId))
                .build());
    }

    @GetMapping("/salaries/summary")
    @PreAuthorize("hasAuthority('TRANSPORT_SALARY_VIEW')")
    @Operation(summary = "Per-worker salary totals for a month")
    public ResponseEntity<?> salarySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(RestApiResponse.<List<WorkerSalarySummaryResponse>>builder()
                .message("Worker salary summary fetched")
                .data(salaryService.summary(year, month))
                .build());
    }

    @PostMapping("/salaries/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Approve pending worker salaries (SUPER_ADMIN / ADMIN)")
    public ResponseEntity<?> approveSalaries(@Valid @RequestBody SalaryDecisionDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<List<WorkerSalaryResponse>>builder()
                .message("Worker salaries approved")
                .data(salaryService.approve(dto))
                .build());
    }

    @PostMapping("/salaries/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Reject pending worker salaries (SUPER_ADMIN / ADMIN)")
    public ResponseEntity<?> rejectSalaries(@Valid @RequestBody SalaryDecisionDTO dto) {
        return ResponseEntity.ok(RestApiResponse.<List<WorkerSalaryResponse>>builder()
                .message("Worker salaries rejected")
                .data(salaryService.reject(dto))
                .build());
    }

    // ---------- Settings ----------

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('TRANSPORT_SALARY_VIEW')")
    @Operation(summary = "Transport settings (worker salary percent)")
    public ResponseEntity<?> settings() {
        return ok("Transport settings fetched", salaryService.getSetting());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update worker salary percent (SUPER_ADMIN / ADMIN)")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody TransportSettingDTO dto) {
        return ok("Transport settings updated", salaryService.updateSetting(dto));
    }

    private <T> ResponseEntity<RestApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(RestApiResponse.<T>builder().message(message).data(data).build());
    }

    private <T> ResponseEntity<RestApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestApiResponse.<T>builder().message(message).data(data).build());
    }
}
