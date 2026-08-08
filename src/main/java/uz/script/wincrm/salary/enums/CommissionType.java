package uz.script.wincrm.salary.enums;

/**
 * Sotuvchining har bir buyurtmadan oladigan komissiyasi qanday hisoblanishini bildiradi.
 * Admin SalaryConfig'da ushbu turni tanlaydi.
 */
public enum CommissionType {
    /** commissionValue foiz sifatida qaraladi (masalan 2 => har buyurtmadan 2%). */
    PERCENT,
    /** commissionValue aniq summa sifatida qaraladi (har to'liq to'langan buyurtma uchun bir marta). */
    FIXED
}
