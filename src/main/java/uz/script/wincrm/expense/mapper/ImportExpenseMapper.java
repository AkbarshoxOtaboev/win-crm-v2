package uz.script.wincrm.expense.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.currency.CurrencyMath;
import uz.script.wincrm.expense.ImportExpense;
import uz.script.wincrm.expense.response.ImportExpenseResponse;

@Component
public class ImportExpenseMapper {

    public ImportExpenseResponse toResponse(ImportExpense entity) {
        if (entity == null) {
            return null;
        }
        return ImportExpenseResponse.builder()
                .id(entity.getId())
                .warehouseOrderId(entity.getOrderId())
                .warehouseOrderItemId(entity.getItemId())
                .supplierName(entity.getSupplierName())
                .orderArrivalDate(entity.getOrderArrivalDate())
                .kind(entity.getKind())
                .amount(entity.getAmount())
                .currency(CurrencyMath.orBase(entity.getCurrency()))
                .exchangeRate(entity.getExchangeRate())
                .paymentTypeId(entity.getPaymentType() != null ? entity.getPaymentType().getId() : null)
                .paymentTypeName(entity.getPaymentType() != null ? entity.getPaymentType().getName() : null)
                .cashSource(entity.getCashSource() != null ? entity.getCashSource() : CashSource.OPERATIONAL)
                .expenseDate(entity.getExpenseDate())
                .description(entity.getDescription())
                .positionGoodsName(entity.getPositionGoodsName())
                .positionWidth(entity.getPositionWidth())
                .positionHeight(entity.getPositionHeight())
                .positionPieces(entity.getPositionPieces())
                .positionQty(entity.getPositionQty())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
