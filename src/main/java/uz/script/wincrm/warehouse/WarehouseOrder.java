package uz.script.wincrm.warehouse;

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
import uz.script.wincrm.suppliers.Supplier;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.enums.WarehouseOrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SQLRestriction("status <> 'DELETED'")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.WAREHOUSE_ORDERS)
public class WarehouseOrder extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private String comment;

    @Column(nullable = false)
    private LocalDateTime arrivalDate;

    private BigDecimal totalSum;

    /** Ixtiyoriy xizmat haqi — totalSum = pozitsiyalar + serviceFee */
    private BigDecimal serviceFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private WarehouseOrderStatus orderStatus;

    /** Narxlar, totalSum va serviceFee shu valyutada. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    /** 1 birlik valyuta = exchangeRate so'm (UZS uchun 1); omborga tannarx shu kurs bilan so'mda tushadi. */
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 4)
    @ColumnDefault("1")
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** Hujjat valyutasidagi summani so'mga o'giradi. */
    public BigDecimal toBase(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (currency == null || currency.isBase() || exchangeRate == null) {
            return amount;
        }
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

}
