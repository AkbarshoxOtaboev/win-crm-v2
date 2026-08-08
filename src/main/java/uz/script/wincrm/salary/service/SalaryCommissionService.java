package uz.script.wincrm.salary.service;

import uz.script.wincrm.sale.SaleOrder;

/**
 * Komissiya hisoblash "dvigateli". PaymentServiceImpl'dan (to'lov yaratilgan/yangilangan/
 * o'chirilgan hodisalarda, paidSum/debtSum qayta hisoblangandan KEYIN) chaqiriladi.
 *
 * Yondashuv - single source of truth (recalculateSaleOrderSums / recalculateClientBalance
 * pattern'ingizdek): har safar buyurtma bo'yicha KUTILGAN komissiya qayta hisoblanadi va
 * allaqachon kitobga olingan komissiya bilan farqi (delta) yoziladi. Shu tufayli to'lov
 * qo'shilishi, kamayishi, buyurtma bekor bo'lishi - barchasi avtomatik to'g'rilanadi,
 * alohida "reversal" mantiqi yozish shart emas.
 */
public interface SalaryCommissionService {

    /**
     * Berilgan buyurtma uchun komissiyani qayta hisoblab, farq bo'lsa COMMISSION yoki
     * COMMISSION_REVERSAL yozuvini yaratadi. Idempotent: farq bo'lmasa hech narsa yozmaydi.
     */
    void recalculateCommissionForSaleOrder(SaleOrder saleOrder);
}
