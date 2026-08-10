package uz.script.wincrm.sale.service;

import org.springframework.web.multipart.MultipartFile;
import uz.script.wincrm.sale.enums.SaleOrderImageType;
import uz.script.wincrm.sale.response.SaleOrderImageResponse;

import java.util.List;

public interface SaleOrderImageService {

    /**
     * Bitta buyurtmaga bir nechta rasm yuklaydi. imageType ixtiyoriy (null bo'lsa OTHER).
     */
    List<SaleOrderImageResponse> uploadImages(Long saleOrderId, MultipartFile[] files, SaleOrderImageType imageType);

    /**
     * Buyurtmaning barcha rasmlarini (xronologik) qaytaradi.
     */
    List<SaleOrderImageResponse> fetchBySaleOrderId(Long saleOrderId);

    /**
     * Bitta rasmni soft-delete qiladi va fayl storagedan o'chiriladi.
     */
    void deleteImage(Long saleOrderId, Long imageId);
}