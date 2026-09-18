package uz.script.wincrm.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    @Override
    public void save(AuditLog auditLog) {
        auditLog.setCreatedAt(LocalDateTime.now());
        repository.save(auditLog);
    }

    @Override
    public Page<AuditResponse> findAll(String username, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return repository.findAll(AuditLogSpecification.filter(username, fromDate, toDate), pageable)
                .map(this::toResponse);
    }

    private AuditResponse toResponse(AuditLog log) {
        return AuditResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .entity(log.getEntity())
                .action(log.getAction())
                .httpMethod(log.getHttpMethod())
                .requestUrl(log.getRequestUrl())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .ipAddress(log.getIpAddress())
                .build();
    }
}
