package uz.script.wincrm.salary.service;

import uz.script.wincrm.sale.SaleOrder;

/**
 * Komissiya hisoblash. Har safar kutilgan summa qayta hisoblanadi va kitobdagi
 * summa bilan farqi yoziladi. Qo'shimcha to'lov, kamayish va bekor qilish shu
 * farq orqali tuzatiladi.
 */
public interface SalaryCommissionService {

    /**
     * Sotuvchi komissiyasi. SELLER lavozimi, buyurtma yetkazilgan (optomda WORK_DONE)
     * va kamida bitta to'lov bo'lsa yoziladi. Foiz to'langan summadan, aniq summa
     * har shunday buyurtma uchun bir marta.
     */
    void recalculateCommissionForSaleOrder(SaleOrder saleOrder);

    /**
     * Transport menejer haqi. Yetkazish tasdiqlanganda yoziladi, bekor qilinsa qaytariladi.
     * Foiz buyurtma summasidan, aniq summa har tasdiqlangan yetkazish uchun bir marta.
     * Ishchilar ulushi bu yerga kirmaydi.
     */
    void recalculateTransportManagerCommission(SaleOrder saleOrder, boolean delivered);
}
