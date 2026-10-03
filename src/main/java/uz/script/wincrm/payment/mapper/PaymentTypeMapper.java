package uz.script.wincrm.payment.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.payment.dto.PaymentTypeDTO;
import uz.script.wincrm.payment.response.PaymentTypeResponse;

@Component
public class PaymentTypeMapper {

    public PaymentType toEntity(PaymentTypeDTO dto) {
        if (dto == null) {
            return null;
        }
        return PaymentType.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : Currency.UZS)
                .build();
    }

    public void updateEntity(PaymentType entity, PaymentTypeDTO dto) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            entity.setIcon(dto.getIcon());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
    }

    public PaymentTypeResponse toResponse(PaymentType entity) {
        if (entity == null) {
            return null;
        }
        return PaymentTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdUsername(entity.getCreatedUsername())
                .build();
    }
}