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
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ta'minotchi bilan hisob-kitob: har bir valyuta uchun alohida qator, valyutalar hech qachon qo'shilmaydi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED' ")
@Table(
        name = TableName.SUPPLIER_BALANCE,
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_balance_currency", columnNames = {"supplier_id", "currency"})
)
public class SupplierBalance extends FilialScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    @Column(nullable = false)
    private BigDecimal totalPurchase = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalDebt = BigDecimal.ZERO;

    private LocalDateTime lastUpdated;
}
