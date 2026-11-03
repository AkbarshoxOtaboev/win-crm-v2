package uz.script.wincrm.salary.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.salary.SalaryConfig;
import uz.script.wincrm.salary.response.SalaryConfigResponse;
import uz.script.wincrm.users.User;

import java.util.Comparator;
import java.util.List;

@Component
public class SalaryConfigMapper {

    public SalaryConfigResponse toResponse(SalaryConfig entity) {
        if (entity == null) {
            return null;
        }
        User user = entity.getUser();
        return SalaryConfigResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .roleNames(roleNames(user))
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

    private List<String> roleNames(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
