package uz.script.wincrm.workshop.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.workshop.Workshop;
import uz.script.wincrm.workshop.enums.WorkshopPayBasis;

import java.math.BigDecimal;
import uz.script.wincrm.workshop.dto.WorkshopDTO;
import uz.script.wincrm.workshop.response.WorkshopResponse;

@Component
public class WorkshopMapper {

    public Workshop toEntity(WorkshopDTO dto) {
        WorkshopPayBasis basis = basisOf(dto);
        return Workshop.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .payBasis(basis)
                .feePercent(basis == WorkshopPayBasis.PERCENT ? dto.getFeePercent() : BigDecimal.ZERO)
                .pieceRate(basis == WorkshopPayBasis.PERCENT ? null : dto.getPieceRate())
                .status(Status.ACTIVE)
                .build();
    }

    public void updateEntity(Workshop workshop, WorkshopDTO dto) {
        WorkshopPayBasis basis = basisOf(dto);
        workshop.setName(dto.getName());
        workshop.setDescription(dto.getDescription());
        workshop.setPayBasis(basis);
        workshop.setFeePercent(basis == WorkshopPayBasis.PERCENT ? dto.getFeePercent() : BigDecimal.ZERO);
        workshop.setPieceRate(basis == WorkshopPayBasis.PERCENT ? null : dto.getPieceRate());
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
                .feePercent(workshop.getFeePercent())
                .payBasis(workshop.getPayBasis() != null ? workshop.getPayBasis() : WorkshopPayBasis.PERCENT)
                .pieceRate(workshop.getPieceRate())
                .status(workshop.getStatus())
                .createdAt(workshop.getCreatedAt())
                .updatedAt(workshop.getUpdatedAt())
                .createdUsername(workshop.getCreatedUsername())
                .build();
    }

    private WorkshopPayBasis basisOf(WorkshopDTO dto) {
        return dto.getPayBasis() != null ? dto.getPayBasis() : WorkshopPayBasis.PERCENT;
    }
}
