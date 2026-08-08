package uz.script.wincrm.salary.enums;

/**
 * SalaryTransaction (oylik ledger) yozuvining turi.
 * Barcha amount qiymatlari MUSBAT magnitud sifatida saqlanadi; oylik yakuniy
 * summada ishorasi entryType orqali qo'llaniladi:
 *   net = baseSalary + COMMISSION - COMMISSION_REVERSAL + BONUS - DEDUCTION - ADVANCE
 */
public enum SalaryEntryType {
    /** Buyurtmadan hisoblangan komissiya (avtomatik, to'lov kelganda). */
    COMMISSION,
    /** Avval yozilgan komissiyaning qaytarilishi (buyurtma bekor/qaytarilsa yoki to'lov kamaysa). */
    COMMISSION_REVERSAL,
    /** Qo'lda beriladigan qo'shimcha mukofot. */
    BONUS,
    /** Qo'lda ushlab qolinadigan summa (jarima, kamomad va h.k.). */
    DEDUCTION,
    /** Oldindan berilgan avans (oylikdan ushlab qolinadi). */
    ADVANCE
}
