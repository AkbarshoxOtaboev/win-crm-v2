package uz.script.wincrm.salary.enums;

/**
 * SalaryTransaction (oylik ledger) yozuvining turi.
 * Barcha amount qiymatlari MUSBAT magnitud sifatida saqlanadi; oylik yakuniy
 * summada ishorasi entryType orqali qo'llaniladi:
 *   net = baseSalary
 *       + COMMISSION - COMMISSION_REVERSAL
 *       + TRANSPORT_COMMISSION - TRANSPORT_COMMISSION_REVERSAL
 *       + BONUS - DEDUCTION - ADVANCE
 */
public enum SalaryEntryType {
    /** Sotuvchi komissiyasi: yetkazilgan buyurtmaning to'langan qismidan. */
    COMMISSION,
    /** Avval yozilgan sotuvchi komissiyasining qaytarilishi. */
    COMMISSION_REVERSAL,
    /** Transport menejer haqi: yetkazish tasdiqlanganda. */
    TRANSPORT_COMMISSION,
    /** Transport menejer haqining qaytarilishi (yetkazish bekor qilinsa). */
    TRANSPORT_COMMISSION_REVERSAL,
    /** Qo'lda beriladigan qo'shimcha mukofot. */
    BONUS,
    /** Qo'lda ushlab qolinadigan summa (jarima, kamomad va h.k.). */
    DEDUCTION,
    /** Oldindan berilgan avans (oylikdan ushlab qolinadi). */
    ADVANCE
}
