package uz.script.wincrm.clients.service;

import uz.script.wincrm.clients.dto.ClientBalanceDTO;
import uz.script.wincrm.clients.response.ClientBalanceResponse;

import java.time.LocalDate;
import java.util.List;

/** Balanslar valyuta bo'yicha alohida qatorlarda qaytariladi: har bir (mijoz, valyuta) - bitta qator. */
public interface ClientBalanceService {

    /**
     * @param fromDate davr boshlanishi (null bo'lsa - joriy oyning 1-kuni)
     * @param toDate   davr oxiri (null bo'lsa - joriy oyning oxirgi kuni)
     */
    List<ClientBalanceResponse> fetchAll(LocalDate fromDate, LocalDate toDate);

    /**
     * @param fromDate davr boshlanishi (null bo'lsa - joriy oyning 1-kuni)
     * @param toDate   davr oxiri (null bo'lsa - joriy oyning oxirgi kuni)
     */
    List<ClientBalanceResponse> findByClientId(Long clientId, LocalDate fromDate, LocalDate toDate);

    /**
     * Davr bilan bog'liq bo'lmagan, bazada saqlangan barcha vaqt (all-time) balanslarini qaytaradi.
     * Hech bir balans bo'lmasa - so'mdagi nol qator.
     */
    List<ClientBalanceResponse> findAllTimeByClientId(Long clientId);

    /**
     * SaleOrder va Payment yozuvlaridan har bir valyuta bo'yicha summalarni qayta yig'ib,
     * ClientBalance qatorlarini (mavjud bo'lmasa - yaratib) yangilaydi.
     */
    void recalculateClientBalance(Long clientId);

    ClientBalanceResponse adjustBalance(Long clientId, ClientBalanceDTO dto);
}
