package uz.script.wincrm.cash;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.payment.PaymentType;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Xodim kun yakunida bitta kassadagi (to'lov turi) pulni rahbarga topshirishi.
 * expectedAmount topshirish paytidagi tizim hisobi sifatida saqlanadi.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.CASH_HANDOVERS)
public class CashHandover extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_type_id", nullable = false)
    private PaymentType paymentType;

    /** Tushum qaysi ish kuniga tegishli. */
    @Column(nullable = false)
    private LocalDate handoverDate;

    /** Kassa valyutasi - to'lov turidan olinadi. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    /** Tizim bo'yicha xodimda qolgan summa (kirim - chiqim - avvalgi topshirishlar). */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    /** Xodim amalda topshirgan summa. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashHandoverStatus handoverStatus;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    private LocalDateTime reviewedAt;

    private String reviewComment;
}
