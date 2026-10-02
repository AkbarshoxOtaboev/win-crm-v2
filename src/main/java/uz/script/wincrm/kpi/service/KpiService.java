package uz.script.wincrm.kpi.service;

import uz.script.wincrm.kpi.response.KpiEntryResponse;
import uz.script.wincrm.kpi.response.KpiRateResponse;
import uz.script.wincrm.kpi.response.KpiSummaryResponse;
import uz.script.wincrm.sale.SaleOrder;

import java.math.BigDecimal;
import java.util.List;

public interface KpiService {

    /** Buyurtma yakunlanganda sotuvchiga KPI yozadi (bir buyurtmaga bir marta). */
    void accrueForSaleOrder(SaleOrder saleOrder);

    void removeForSaleOrder(Long saleOrderId);

    List<KpiRateResponse> fetchRates();

    KpiRateResponse setRate(Long userId, BigDecimal percent);

    List<KpiSummaryResponse> summary(int year, int month);

    /** month null bo'lsa - barcha davrlar. */
    List<KpiEntryResponse> userEntries(Long userId, int year, Integer month);

    List<KpiSummaryResponse> workshopSummary(int year, int month);

    List<KpiEntryResponse> workshopEntries(Long workshopId, int year, Integer month);
}
