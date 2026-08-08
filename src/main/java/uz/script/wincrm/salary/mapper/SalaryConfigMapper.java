package uz.script.wincrm.salary.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.response.SalaryConfigResponse;

@Component
public class SalaryConfigMapper {

    public SalaryConfigResponse toResponse(SalaryConfig entity) {
        if (entity == null) {
            return null;
        }
        return SalaryConfigResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userFullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .baseSalary(entity.getBaseSalary())
                .commissionType(entity.getCommissionType())
                .commissionValue(entity.getCommissionValue())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdUsername(entity.getCreatedUsername())
                .build();
    }
}
