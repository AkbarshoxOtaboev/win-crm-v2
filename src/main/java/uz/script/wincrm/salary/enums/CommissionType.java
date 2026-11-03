package uz.script.wincrm.salary.enums;

/**
 * Sotuvchining har bir buyurtmadan oladigan komissiyasi qanday hisoblanishini bildiradi.
 * Admin SalaryConfig'da ushbu turni tanlaydi.
 */
public enum CommissionType {
    /** commissionValue foiz. Sotuvchida to'langan summadan, transport menejerda buyurtma summasidan. */
    PERCENT,
    /** commissionValue aniq summa: sotuvchida har yetkazilgan buyurtma, transportda har tasdiqlangan yetkazish. */
    FIXED
}
