package uz.script.wincrm.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.cash.CashSource;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kirim buyurtmasiga yozilgan boj yoki yo'l haqi. Summa butun kirim uchun.
 * Pozitsiyalarga bo'linishi alohida yozuvlarda turadi.
 * Oddiy xarajatlar ro'yxatiga kirmaydi. Yetkazuvchi qarzi va kirim summasini o'zgartirmaydi.
 * Pul tanlangan kassadan chiqadi. Pozitsiya keyin o'chsa, saqlangan nom qoladi.
 */
@Entity
@Table(name = TableName.IMPORT_EXPENSE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
public class ImportExpense extends FilialScopedEntity {

    /** Kirim buyurtma. Alohida ustun: buyurtma o'chsa ham yozuv ro'yxatda qoladi. */
    @Column(name = "warehouse_order_id", nullable = false)
    private Long orderId;

    /** Eski yozuvlar: summa bitta pozitsiyaga yozilgan bo'lsa. Yangi yozuvlarda bo'sh. */
    @Column(name = "warehouse_order_item_id")
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportExpenseKind kind;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    @Column(precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cash_source", nullable = false, length = 20)
    @ColumnDefault("'OPERATIONAL'")
    @Builder.Default
    private CashSource cashSource = CashSource.OPERATIONAL;

    /** Yozuv paytidagi yetkazuvchi nomi. */
    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "order_arrival_date")
    private LocalDateTime orderArrivalDate;

    @Column(name = "position_goods_name", length = 255)
    private String positionGoodsName;

    @Column(name = "position_width", precision = 38, scale = 4)
    private BigDecimal positionWidth;

    @Column(name = "position_height", precision = 38, scale = 4)
    private BigDecimal positionHeight;

    /** Dona. Oyna (eni/bo'yi bor) pozitsiyada to'ldiriladi. */
    @Column(name = "position_pieces", precision = 38, scale = 4)
    private BigDecimal positionPieces;

    /** Ombor miqdori: oyna uchun kv.m, qolganlari uchun dona. */
    @Column(name = "position_qty", precision = 38, scale = 4)
    private BigDecimal positionQty;
}
