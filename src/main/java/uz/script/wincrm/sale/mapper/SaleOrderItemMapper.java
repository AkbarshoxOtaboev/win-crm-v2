package uz.script.wincrm.sale.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.GoodsCategory;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.dto.SaleOrderItemDTO;
import uz.script.wincrm.sale.response.SaleOrderItemResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SaleOrderItemMapper {

    public SaleOrderItem toEntity(SaleOrderItemDTO dto) {
        if (dto == null) {
            return null;
        }
        return SaleOrderItem.builder()
                .priceCost(dto.getPriceCost())
                .priceSelling(dto.getPriceSelling())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .count(dto.getCount())
                .cutKerfMm(dto.getCutKerfMm())
                .cutAllowRotate(dto.getCutAllowRotate())
                .cutMinRemnantCm(dto.getCutMinRemnantCm())
                .cutCandidateGoodsIds(cleanCandidates(dto.getCutCandidateGoodsIds()))
                .cutCategory(categoryRef(dto.getCutCategoryId()))
                .arrivalDate(dto.getArrivalDate())
                .build();
    }

    public void updateEntity(SaleOrderItem entity, SaleOrderItemDTO dto) {
        if (dto.getPriceCost() != null) {
            entity.setPriceCost(dto.getPriceCost());
        }
        if (dto.getPriceSelling() != null) {
            entity.setPriceSelling(dto.getPriceSelling());
        }
        if (dto.getWidth() != null) {
            entity.setWidth(dto.getWidth());
        }
        if (dto.getHeight() != null) {
            entity.setHeight(dto.getHeight());
        }
        if (dto.getCount() != null) {
            entity.setCount(dto.getCount());
        }
        if (dto.getArrivalDate() != null) {
            entity.setArrivalDate(dto.getArrivalDate());
        }
        if (dto.getCutKerfMm() != null) {
            entity.setCutKerfMm(dto.getCutKerfMm());
        }
        if (dto.getCutAllowRotate() != null) {
            entity.setCutAllowRotate(dto.getCutAllowRotate());
        }
        if (dto.getCutMinRemnantCm() != null) {
            entity.setCutMinRemnantCm(dto.getCutMinRemnantCm());
        }
        if (dto.getCutCandidateGoodsIds() != null) {
            entity.setCutCandidateGoodsIds(cleanCandidates(dto.getCutCandidateGoodsIds()));
        }
        if (dto.getCutCategoryId() != null) {
            entity.setCutCategory(dto.getCutCategoryId() <= 0 ? null : categoryRef(dto.getCutCategoryId()));
        }
    }

    private String cleanCandidates(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9,]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private GoodsCategory categoryRef(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        GoodsCategory category = new GoodsCategory();
        category.setId(id);
        return category;
    }

    public SaleOrderItemResponse toResponse(SaleOrderItem entity) {
        if (entity == null) {
            return null;
        }
        return SaleOrderItemResponse.builder()
                .id(entity.getId())
                .warehouseId(entity.getWarehouse() != null ? entity.getWarehouse().getId() : null)
                .warehouseName(entity.getWarehouse() != null ? entity.getWarehouse().getName() : null)
                .saleOrderId(entity.getSaleOrder() != null ? entity.getSaleOrder().getId() : null)
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientFullName(entity.getClient() != null ? entity.getClient().getFullName() : null)
                .goodsId(entity.getGoods() != null ? entity.getGoods().getId() : null)
                .goodsName(entity.getGoods() != null ? entity.getGoods().getName() : null)
                .priceCost(entity.getPriceCost())
                .priceSelling(entity.getPriceSelling())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .count(entity.getCount())
                .weightKg(weightKg(entity))
                .cutKerfMm(entity.getCutKerfMm())
                .cutAllowRotate(entity.getCutAllowRotate())
                .cutMinRemnantCm(entity.getCutMinRemnantCm())
                .cutCandidateGoodsIds(entity.getCutCandidateGoodsIds())
                .cutCategoryId(entity.getCutCategory() != null ? entity.getCutCategory().getId() : null)
                .arrivalDate(entity.getArrivalDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdUsername(entity.getCreatedUsername())
                .build();
    }

    /** WINDOW: vazn = kv.m (count) × 1 m² vazni. */
    private BigDecimal weightKg(SaleOrderItem entity) {
        Goods goods = entity.getGoods();
        if (goods == null || goods.getType() != Type.WINDOW) {
            return null;
        }
        BigDecimal kvm = entity.getCount();
        BigDecimal per = goods.getWeightPerSqm();
        if (kvm == null || per == null || kvm.signum() <= 0 || per.signum() <= 0) {
            return null;
        }
        return kvm.multiply(per).setScale(2, RoundingMode.HALF_UP);
    }
}