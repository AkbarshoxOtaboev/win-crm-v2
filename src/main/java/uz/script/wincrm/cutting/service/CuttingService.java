package uz.script.wincrm.cutting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.cutting.CuttingCalculator;
import uz.script.wincrm.cutting.CuttingInput;
import uz.script.wincrm.cutting.dto.CuttingCalculateRequest;
import uz.script.wincrm.cutting.response.CuttingResultResponse;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.goods.repository.GoodsRepository;

import java.math.BigDecimal;

@Service
public class CuttingService {

    private final GoodsRepository goodsRepository;
    private final BigDecimal defaultKerfMm;
    private final BigDecimal defaultMinRemnantCm;

    public CuttingService(
            GoodsRepository goodsRepository,
            @Value("${app.cutting.kerf-mm:3}") BigDecimal defaultKerfMm,
            @Value("${app.cutting.min-remnant-cm:30}") BigDecimal defaultMinRemnantCm
    ) {
        this.goodsRepository = goodsRepository;
        this.defaultKerfMm = defaultKerfMm;
        this.defaultMinRemnantCm = defaultMinRemnantCm;
    }

    @Transactional(readOnly = true)
    public CuttingResultResponse calculate(CuttingCalculateRequest request) {
        BigDecimal sheetWidth = request.getSheetWidth();
        BigDecimal sheetHeight = request.getSheetHeight();
        if ((sheetWidth == null || sheetHeight == null) && request.getGoodsId() != null) {
            Goods goods = goodsRepository.findByIdActive(request.getGoodsId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mahsulot topilmadi: " + request.getGoodsId()));
            if (goods.getType() != Type.WINDOW) {
                throw new BadRequestException("Kesish hisobi faqat oyna mahsulotlari uchun");
            }
            sheetWidth = goods.getWidth();
            sheetHeight = goods.getHeight();
        }
        if (sheetWidth == null || sheetHeight == null) {
            throw new BadRequestException("List o'lchami noma'lum: mahsulotda eni va bo'yi kiritilmagan");
        }
        return CuttingCalculator.calculate(new CuttingInput(
                sheetWidth,
                sheetHeight,
                request.getPieceWidth(),
                request.getPieceHeight(),
                request.getQuantity(),
                request.getKerfMm() != null ? request.getKerfMm() : defaultKerfMm,
                request.getAllowRotate() == null || request.getAllowRotate(),
                request.getMinRemnantCm() != null ? request.getMinRemnantCm() : defaultMinRemnantCm
        ));
    }
}
