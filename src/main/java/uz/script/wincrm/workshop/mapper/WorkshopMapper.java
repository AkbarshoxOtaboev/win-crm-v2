package uz.script.wincrm.workshop.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.dto.WorkshopDTO;
import uz.script.wincrm.workshop.response.WorkshopResponse;

@Component
public class WorkshopMapper {

    public Workshop toEntity(WorkshopDTO dto) {
        return Workshop.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .status(Status.ACTIVE)
                .build();
    }

    public void updateEntity(Workshop workshop, WorkshopDTO dto) {
        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
    }

    public WorkshopResponse toResponse(Workshop workshop) {
        User manager = workshop.getManager();
        return WorkshopResponse.builder()
                .id(workshop.getId())
                .name(workshop.getName())
                .description(workshop.getDescription())
                .managerId(manager != null ? manager.getId() : null)
                .managerFullName(manager != null
                        ? (manager.getFullName() != null ? manager.getFullName() : manager.getUsername())
                        : null)
                .status(workshop.getStatus())
                .createdAt(workshop.getCreatedAt())
                .updatedAt(workshop.getUpdatedAt())
                .createdUsername(workshop.getCreatedUsername())
                .build();
    }
}
