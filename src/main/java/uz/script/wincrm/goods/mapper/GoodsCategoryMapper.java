package uz.script.wincrm.goods.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.goods.GoodsCategory;
import uz.script.wincrm.goods.dto.GoodsCategoryDTO;
import uz.script.wincrm.goods.response.GoodsCategoryResponse;
import uz.script.wincrm.utils.Status;

@Component
public class GoodsCategoryMapper {

    public GoodsCategory toEntity(GoodsCategoryDTO dto) {
        GoodsCategory entity = new GoodsCategory();
        entity.setName(dto.getName().trim());
        entity.setStatus(Status.ACTIVE);
        return entity;
    }

    public void updateEntity(GoodsCategory entity, GoodsCategoryDTO dto) {
        entity.setName(dto.getName().trim());
    }

    public GoodsCategoryResponse toResponse(GoodsCategory entity) {
        if (entity == null) {
            return null;
        }
        return GoodsCategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createdUsername(entity.getCreatedUsername())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
