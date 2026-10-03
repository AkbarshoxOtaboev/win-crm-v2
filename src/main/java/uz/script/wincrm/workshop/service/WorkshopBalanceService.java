package uz.script.wincrm.workshop.service;

import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;
import uz.script.wincrm.workshop.response.WorkshopDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface WorkshopBalanceService {

    void creditForAssignment(ProductionAssignment assignment, WorkshopBalanceEventType eventType);

    void setAssignmentFeePercent(Long assignmentId, BigDecimal feePercent);

    /** Yakunlangan ishlar ro'yxati va jami [fromDate, toDate] bo'yicha; navbat va balans davrga bog'liq emas. */
    WorkshopDashboardResponse dashboard(Long workshopId, LocalDate fromDate, LocalDate toDate);
}
