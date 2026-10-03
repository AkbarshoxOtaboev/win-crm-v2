package uz.script.wincrm.suppliers;

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
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.SUPPLIER_PAYMENTS)
@SQLRestriction("status <> 'DELETED'")
public class SupplierPayment extends FilialScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    private BigDecimal paidSumm;
    private LocalDateTime paidDate;
    private String comment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    /** paidSumm valyutasi (to'lov turi kassasining valyutasi). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    /** Qaysi valyutadagi qarz yopildi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "debt_currency", nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency debtCurrency = Currency.UZS;

    /** To'lov kunidagi kurs: 1 xorijiy birlik = exchangeRate so'm (ikkalasi UZS bo'lsa 1). */
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("1")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** debtCurrency'dagi yopilgan summa. */
    @Column(name = "applied_amount")
    private BigDecimal appliedAmount;

    public BigDecimal appliedOrPaid() {
        return appliedAmount != null ? appliedAmount : paidSumm;
    }
}
