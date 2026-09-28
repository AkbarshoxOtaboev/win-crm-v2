package uz.script.wincrm.transport.enums;

public enum DeliveryStatus {
    /** Sotuv buyurtmasi "Yetkazib berishda" ga o'tdi, transport menejeri qabul qilmagan. */
    PENDING,
    /** Transport menejeri qabul qildi, haydovchi va ishchilar biriktirildi. */
    ACCEPTED,
    /** Yo'lga chiqdi. */
    IN_TRANSIT,
    /** Transport mijozga yetkazganini belgiladi, sotuv menejeri tasdig'i kutilmoqda. */
    ARRIVED,
    /** Sotuv menejeri yetkazilganini tasdiqladi, ishchilarga oylik hisoblandi. */
    CONFIRMED,
    CANCELLED
}
