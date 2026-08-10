package uz.script.wincrm.sale.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.script.wincrm.audit.AuditAction;
import uz.script.wincrm.audit.Auditable;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderImage;
import uz.script.wincrm.sale.enums.SaleOrderImageType;
import uz.script.wincrm.sale.mapper.SaleOrderImageMapper;
import uz.script.wincrm.sale.repository.SaleOrderImageRepository;
import uz.script.wincrm.sale.repository.SaleOrderRepository;
import uz.script.wincrm.sale.response.SaleOrderImageResponse;
import uz.script.wincrm.sale.service.SaleOrderImageService;
import uz.script.wincrm.storage.StorageService;
import uz.script.wincrm.utils.Status;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleOrderImageServiceImpl implements SaleOrderImageService {

    private final SaleOrderImageRepository imageRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderImageMapper imageMapper;
    private final StorageService storageService;

    /**
     * Ruxsat etilgan rasm MIME turlari. Faqat rasm fayllar qabul qilinadi.
     */
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    @Override
    @Auditable(
            action = AuditAction.CREATE,
            entity = "SaleOrderImage"
    )
    public List<SaleOrderImageResponse> uploadImages(Long saleOrderId, MultipartFile[] files, SaleOrderImageType imageType) {
        log.info("Upload {} image(s) to sale order {}", files != null ? files.length : 0, saleOrderId);

        SaleOrder saleOrder = saleOrderRepository.findById(saleOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + saleOrderId));

        if (files == null || files.length == 0) {
            throw new BadRequestException("Kamida bitta rasm fayl yuborilishi kerak");
        }

        SaleOrderImageType type = imageType != null ? imageType : SaleOrderImageType.OTHER;

        List<SaleOrderImageResponse> result = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null || ALLOWED_CONTENT_TYPES.stream().noneMatch(contentType::equalsIgnoreCase)) {
                throw new BadRequestException("Faqat rasm fayllar yuklash mumkin. Noto'g'ri fayl: "
                        + file.getOriginalFilename());
            }

            // StorageService faylni diskka saqlaydi va UUID.ext nomini qaytaradi
            String storedFileName = storageService.uploadFile(file);

            SaleOrderImage image = SaleOrderImage.builder()
                    .saleOrder(saleOrder)
                    .fileName(storedFileName)
                    .originalFileName(file.getOriginalFilename())
                    .contentType(contentType)
                    .size(file.getSize())
                    .imageType(type)
                    .status(Status.ACTIVE)
                    .build();

            image = imageRepository.save(image);
            result.add(imageMapper.toResponse(image));
        }

        if (result.isEmpty()) {
            throw new BadRequestException("Yuklash uchun yaroqli rasm topilmadi");
        }

        return result;
    }

    @Override
    public List<SaleOrderImageResponse> fetchBySaleOrderId(Long saleOrderId) {
        log.info("Fetch images of sale order {}", saleOrderId);

        // buyurtma mavjudligini tekshirish
        saleOrderRepository.findById(saleOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale order not found with id: " + saleOrderId));

        return imageRepository.findBySaleOrderIdOrderByCreatedAtAsc(saleOrderId)
                .stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @Override
    @Auditable(
            action = AuditAction.DELETE,
            entity = "SaleOrderImage"
    )
    public void deleteImage(Long saleOrderId, Long imageId) {
        log.info("Delete image {} of sale order {}", imageId, saleOrderId);

        SaleOrderImage image = imageRepository.findByIdAndSaleOrderId(imageId, saleOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image not found with id: " + imageId + " for sale order: " + saleOrderId));

        // avval DB'da soft-delete
        image.setStatus(Status.DELETED);
        imageRepository.save(image);

        // so'ngra fizik faylni storagedan o'chirish (xatolik bo'lsa ham DB soft-delete saqlanadi)
        try {
            storageService.delete(image.getFileName());
        } catch (Exception e) {
            log.warn("Fizik faylni o'chirishda xatolik: {} - {}", image.getFileName(), e.getMessage());
        }
    }
}