package uz.script.wincrm.sale.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.script.wincrm.sale.enums.SaleOrderImageType;
import uz.script.wincrm.sale.response.SaleOrderImageResponse;
import uz.script.wincrm.sale.service.SaleOrderImageService;
import uz.script.wincrm.utils.RestApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sale-orders/{saleOrderId}/images")
public class SaleOrderImageController {

    private final SaleOrderImageService imageService;

    @Operation(
            summary = "Buyurtmaga rasm(lar) yuklash",
            description = "Bitta buyurtmaga bir yoki bir nechta rasm yuklaydi (obyekt rasmi, loyiha/chizma va h.k.). " +
                    "imageType ixtiyoriy."
    )
    @ApiResponse(responseCode = "200", description = "Rasmlar muvaffaqiyatli yuklandi")
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('SALE_ORDER_WRITE')")
    public ResponseEntity<RestApiResponse<List<SaleOrderImageResponse>>> uploadImages(
            @PathVariable Long saleOrderId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "imageType", required = false) SaleOrderImageType imageType
    ) {
        List<SaleOrderImageResponse> result = imageService.uploadImages(saleOrderId, files, imageType);

        return ResponseEntity.ok(
                RestApiResponse.<List<SaleOrderImageResponse>>builder()

                        .message("Rasmlar muvaffaqiyatli yuklandi")
                        .data(result)
                        .build()
        );
    }

    @Operation(
            summary = "Buyurtma rasmlarini olish",
            description = "Berilgan buyurtmaga biriktirilgan barcha rasmlarni qaytaradi."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('SALE_ORDER_READ')")
    public ResponseEntity<RestApiResponse<List<SaleOrderImageResponse>>> fetchImages(
            @PathVariable Long saleOrderId
    ) {
        List<SaleOrderImageResponse> result = imageService.fetchBySaleOrderId(saleOrderId);

        return ResponseEntity.ok(
                RestApiResponse.<List<SaleOrderImageResponse>>builder()
                        .message("Buyurtma rasmlari")
                        .data(result)
                        .build()
        );
    }

    @Operation(
            summary = "Buyurtma rasmini o'chirish",
            description = "Bitta rasmni soft-delete qiladi va fizik faylni storagedan o'chiradi."
    )
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('SALE_ORDER_WRITE')")
    public ResponseEntity<RestApiResponse<Void>> deleteImage(
            @PathVariable Long saleOrderId,
            @PathVariable Long imageId
    ) {
        imageService.deleteImage(saleOrderId, imageId);

        return ResponseEntity.ok(
                RestApiResponse.<Void>builder()
                        .message("Rasm o'chirildi")
                        .build()
        );
    }
}