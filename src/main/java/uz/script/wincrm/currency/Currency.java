package uz.script.wincrm.currency;

/** Tizim valyutalari. UZS - asosiy (hisobot) valyuta, kurslar unga nisbatan saqlanadi. */
public enum Currency {
    UZS,
    USD;

    public static final Currency BASE = UZS;

    public boolean isBase() {
        return this == BASE;
    }
}
