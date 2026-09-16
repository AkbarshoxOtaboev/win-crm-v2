package uz.script.wincrm.warehouse.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.suppliers.Supplier;
import uz.script.wincrm.utils.Status;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.warehouse.WarehouseOrder;
import uz.script.wincrm.warehouse.dto.WarehouseOrderDTO;
import uz.script.wincrm.warehouse.enums.WarehouseOrderStatus;
import uz.script.wincrm.warehouse.response.WarehouseOrderResponse;

import java.math.BigDecimal;

@Component
public class WarehouseOrderMapper {

    public WarehouseOrder toEntity(WarehouseOrderDTO dto, Supplier supplier, Warehouse warehouse) {
        BigDecimal serviceFee = normalizeServiceFee(dto.getServiceFee());
        return WarehouseOrder.builder()
                .supplier(supplier)
                .warehouse(warehouse)
                .comment(dto.getComment())
                .arrivalDate(dto.getArrivalDate())
                .serviceFee(serviceFee)
                .totalSum(serviceFee)
                .orderStatus(WarehouseOrderStatus.NEW)
                .status(Status.ACTIVE)
                .build();
    }

    public void updateEntity(WarehouseOrder order, WarehouseOrderDTO dto, Supplier supplier, Warehouse warehouse) {
        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setComment(dto.getComment());
        order.setArrivalDate(dto.getArrivalDate());
        order.setServiceFee(normalizeServiceFee(dto.getServiceFee()));
    }

    public WarehouseOrderResponse toResponse(WarehouseOrder order) {
        return WarehouseOrderResponse.builder()
                .id(order.getId())
                .supplierId(order.getSupplier() != null ? order.getSupplier().getId() : null)
                .supplierName(order.getSupplier() != null ? order.getSupplier().getName() : null)
                .warehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null)
                .warehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null)
                .comment(order.getComment())
                .arrivalDate(order.getArrivalDate())
                .totalSum(order.getTotalSum())
                .serviceFee(order.getServiceFee())
                .orderStatus(order.getOrderStatus())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdUsername(order.getCreatedUsername())
                .build();
    }

    private BigDecimal normalizeServiceFee(BigDecimal serviceFee) {
        if (serviceFee == null || serviceFee.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return serviceFee;
    }
}
