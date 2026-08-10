package uz.script.wincrm.sale.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.sale.SaleOrderImage;
import uz.script.wincrm.sale.response.SaleOrderImageResponse;

@Component
public class SaleOrderImageMapper {

    /**
     * Fayllarni yuklab olish uchun controller yo'li bilan mos bo'lishi kerak:
     * GET /api/files/{fileName}
     */
    private static final String DOWNLOAD_PATH_PREFIX = "/api/files/";

    public SaleOrderImageResponse toResponse(SaleOrderImage entity) {
        if (entity == null) {
            return null;
        }
        return SaleOrderImageResponse.builder()
                .id(entity.getId())
                .saleOrderId(entity.getSaleOrder() != null ? entity.getSaleOrder().getId() : null)
                .fileName(entity.getFileName())
                .originalFileName(entity.getOriginalFileName())
                .downloadUrl(entity.getFileName() != null ? DOWNLOAD_PATH_PREFIX + entity.getFileName() : null)
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .imageType(entity.getImageType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedUserId())
                .build();
    }
}