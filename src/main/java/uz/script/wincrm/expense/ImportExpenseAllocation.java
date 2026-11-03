package uz.script.wincrm.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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

/**
 * Bir kirim + tur (boj/yo'l) + valyuta bo'yicha umumiy taqsimotning bitta pozitsiya ulushi.
 * Bir necha to'lov bir xil guruhga tushadi. Kassaga ta'sir qilmaydi.
 */
@Entity
@Table(name = TableName.IMPORT_EXPENSE_ALLOCATION)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
public class ImportExpenseAllocation extends FilialScopedEntity {

    /** Eski yozuvlar uchun. Yangi taqsimotda bo'sh. */
    @Column(name = "import_expense_id")
    private Long importExpenseId;

    @Column(name = "warehouse_order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ImportExpenseKind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    @Column(name = "warehouse_order_item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(name = "position_goods_name", nullable = false, length = 255)
    private String positionGoodsName;

    @Column(name = "position_width", precision = 38, scale = 4)
    private BigDecimal positionWidth;

    @Column(name = "position_height", precision = 38, scale = 4)
    private BigDecimal positionHeight;

    @Column(name = "position_pieces", precision = 38, scale = 4)
    private BigDecimal positionPieces;

    @Column(name = "position_qty", precision = 38, scale = 4)
    private BigDecimal positionQty;
}
