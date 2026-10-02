package uz.script.wincrm.sale.service;

import uz.script.wincrm.sale.response.SellerDebtResponse;

import java.time.LocalDate;
import java.util.List;

public interface SellerDebtService {

    /**
     * Qarzi qolgan (bekor qilinmagan) buyurtmalarni sotuvchi -> mijoz bo'yicha guruhlaydi.
     * DEBT_NOTIFICATION_VIEW ruxsati bo'lmagan foydalanuvchi faqat o'z buyurtmalarini ko'radi.
     */
    List<SellerDebtResponse> fetchSellerDebts(Long userId, LocalDate startDate, LocalDate endDate);
}
