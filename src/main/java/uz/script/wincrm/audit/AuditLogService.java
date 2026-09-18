package uz.script.wincrm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    void save(AuditLog auditLog);

    Page<AuditResponse> findAll(String username, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}
