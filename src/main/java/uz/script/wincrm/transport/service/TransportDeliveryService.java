package uz.script.wincrm.transport.service;

import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.transport.dto.DeliveryCrewDTO;
import uz.script.wincrm.transport.enums.DeliveryStatus;
import uz.script.wincrm.transport.response.DeliveryResponse;
import uz.script.wincrm.transport.response.TransportDashboardResponse;

import java.util.List;

public interface TransportDeliveryService {

    /** Sotuv buyurtmasi IN_DELIVERY ga o'tganda chaqiriladi. */
    void createForSaleOrder(SaleOrder saleOrder);

    /** Sotuv menejeri IN_DELIVERY -> DELIVERED ni tasdiqlaganda chaqiriladi. */
    void confirmForSaleOrder(SaleOrder saleOrder);

    /** Sotuv buyurtmasi bekor qilinganda yoki o'chirilganda chaqiriladi. */
    void cancelForSaleOrder(Long saleOrderId);

    List<DeliveryResponse> fetchAll(List<DeliveryStatus> statuses);

    DeliveryResponse findById(Long id);

    DeliveryResponse findBySaleOrderId(Long saleOrderId);

    DeliveryResponse accept(Long id, DeliveryCrewDTO dto);

    DeliveryResponse updateCrew(Long id, DeliveryCrewDTO dto);

    DeliveryResponse depart(Long id);

    DeliveryResponse arrive(Long id);

    TransportDashboardResponse dashboard();
}
