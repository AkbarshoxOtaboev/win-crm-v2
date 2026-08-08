package uz.script.wincrm.salary.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.salary.SalaryTransaction;
import uz.script.wincrm.salary.response.SalaryTransactionResponse;

@Component
public class SalaryTransactionMapper {

    public SalaryTransactionResponse toResponse(SalaryTransaction entity) {
        if (entity == null) {
            return null;
        }
        return SalaryTransactionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .saleOrderId(entity.getSaleOrderId())
                .entryType(entity.getEntryType())
                .amount(entity.getAmount())
                .commissionTypeSnapshot(entity.getCommissionTypeSnapshot())
                .rateSnapshot(entity.getRateSnapshot())
                .baseAmountSnapshot(entity.getBaseAmountSnapshot())
                .earnedAt(entity.getEarnedAt())
                .periodYear(entity.getPeriodYear())
                .periodMonth(entity.getPeriodMonth())
                .comment(entity.getComment())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdUsername(entity.getCreatedUsername())
                .build();
    }
}
