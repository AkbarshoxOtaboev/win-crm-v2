package uz.script.wincrm.transport.service;

import uz.script.wincrm.transport.TransportDelivery;
import uz.script.wincrm.transport.dto.SalaryDecisionDTO;
import uz.script.wincrm.transport.dto.TransportSettingDTO;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;
import uz.script.wincrm.transport.response.TransportSettingResponse;
import uz.script.wincrm.transport.response.WorkerSalaryResponse;
import uz.script.wincrm.transport.response.WorkerSalarySummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TransportSalaryService {

    /** Yetkazish tasdiqlanganda biriktirilgan ishchilarga PENDING oylik yozuvlarini yaratadi. */
    void accrueForDelivery(TransportDelivery delivery);

    /** Buyurtma bekor qilinganda hali tasdiqlanmagan oyliklarni REJECTED qiladi. */
    void rejectPendingForDelivery(Long deliveryId, String reason);

    List<WorkerSalaryResponse> fetch(Integer year, Integer month, WorkerSalaryStatus status, Long workerId);

    List<WorkerSalarySummaryResponse> summary(Integer year, Integer month);

    List<WorkerSalaryResponse> approve(SalaryDecisionDTO dto);

    List<WorkerSalaryResponse> reject(SalaryDecisionDTO dto);

    BigDecimal currentPercent();

    TransportSettingResponse getSetting();

    TransportSettingResponse updateSetting(TransportSettingDTO dto);
}
