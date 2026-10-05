package uz.script.wincrm.cash.service;

import uz.script.wincrm.cash.CashHandoverStatus;
import uz.script.wincrm.cash.dto.CashHandoverRequest;
import uz.script.wincrm.cash.response.CashHandoverResponse;
import uz.script.wincrm.cash.response.CashHandoverSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface CashHandoverService {

    /** userId faqat topshirishlarni qabul qiluvchi uchun; boshqalarga doim o'zi. */
    List<CashHandoverSummaryResponse> summary(LocalDate date, Long userId);

    List<CashHandoverResponse> list(LocalDate fromDate, LocalDate toDate, CashHandoverStatus status, Long userId);

    List<CashHandoverResponse> create(CashHandoverRequest request);

    CashHandoverResponse accept(Long id, String comment);

    CashHandoverResponse reject(Long id, String comment);

    void cancel(Long id);
}
