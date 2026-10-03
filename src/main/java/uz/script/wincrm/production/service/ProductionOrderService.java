package uz.script.wincrm.production.service;

import uz.script.wincrm.production.dto.CompleteProductionDTO;
import uz.script.wincrm.production.dto.RedirectProductionDTO;
import uz.script.wincrm.production.dto.SendToProductionDTO;
import uz.script.wincrm.production.response.ProductionEventResponse;
import uz.script.wincrm.production.response.ProductionOrderResponse;

import java.time.LocalDate;
import java.util.List;

public interface ProductionOrderService {
    ProductionOrderResponse sendToProduction(SendToProductionDTO dto);

    ProductionOrderResponse findById(Long id);

    /** Savdo buyurtmasiga tegishli ishlab chiqarish buyurtmasi; yuborilmagan bo'lsa null. */
    ProductionOrderResponse findBySaleOrderId(Long saleOrderId);

    List<ProductionOrderResponse> fetchAll();

    List<ProductionOrderResponse> board(Long workshopId, LocalDate fromDate, LocalDate toDate);

    ProductionOrderResponse start(Long id);

    ProductionOrderResponse redirect(Long id, RedirectProductionDTO dto);

    ProductionOrderResponse complete(Long id, CompleteProductionDTO dto);

    List<ProductionEventResponse> timeline(Long id);

    /** Savdo buyurtmasi bekor qilinganda ochiq ishlab chiqarish buyurtmasini bekor qiladi. */
    void cancelForSaleOrder(Long saleOrderId);
}
