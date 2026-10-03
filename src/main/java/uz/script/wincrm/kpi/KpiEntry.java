package uz.script.wincrm.kpi;

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
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Yakunlangan buyurtmadan xodimga yozilgan KPI (foiz va summa yozilgan paytdagi holatda saqlanadi). */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(
        name = TableName.KPI_ENTRIES,
        uniqueConstraints = @UniqueConstraint(name = "uk_kpi_entry_order_user", columnNames = {"sale_order_id", "user_id"})
)
public class KpiEntry extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_order_id", nullable = false)
    private SaleOrder saleOrder;

    /** Buyurtma summasi (yetkazib berish to'lovisiz), so'mda. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    /** Buyurtma valyutasi; baseAmount = sourceAmount × exchangeRate. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_currency", nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency sourceCurrency = Currency.UZS;

    /** Buyurtma valyutasidagi summa; eski yozuvlarda null (baseAmount bilan teng). */
    @Column(name = "source_amount", precision = 19, scale = 2)
    private BigDecimal sourceAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("1")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false)
    private Integer periodMonth;
}
