package uz.script.wincrm.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.PAYMENTS)
@SQLRestriction("status <> 'DELETED'")
public class Payment extends FilialScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Kassaga tushgan summa, {@link #currency} da. */
    @Column(nullable = false)
    private BigDecimal paymentAmount;

    /** Kassa valyutasi - to'lov turidan olinadi. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    /** Qaysi valyutadagi qarz yopiladi; buyurtmaga bog'langan to'lovda - buyurtma valyutasi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "debt_currency", nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency debtCurrency = Currency.UZS;

    /** To'lov kungi kurs: 1 birlik xorijiy valyuta = exchangeRate so'm (valyutalar bir xil bo'lsa 1). */
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("1")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** Qarzdan yopilgan summa, {@link #debtCurrency} da. Eski yozuvlarda null - paymentAmount bilan teng. */
    @Column(name = "applied_amount")
    private BigDecimal appliedAmount;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id")
    private SaleOrder saleOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    public BigDecimal appliedOrPaid() {
        return appliedAmount != null ? appliedAmount : paymentAmount;
    }
}
