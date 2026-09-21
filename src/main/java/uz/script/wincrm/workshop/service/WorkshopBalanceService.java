package uz.script.wincrm.workshop.service;

import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;
import uz.script.wincrm.workshop.response.WorkshopDashboardResponse;

import java.math.BigDecimal;

public interface WorkshopBalanceService {

    void creditForAssignment(ProductionAssignment assignment, WorkshopBalanceEventType eventType);

    void setAssignmentFeePercent(Long assignmentId, BigDecimal feePercent);

    WorkshopDashboardResponse dashboard(Long workshopId);
}
