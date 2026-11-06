package uz.script.wincrm.cutting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.utils.TableName;

/** Optimizatsiya qarori: qatorning nechta donasi listdan, nechtasi qoldiqdan. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.CUTTING_ALLOCATIONS)
@SQLRestriction("status <> 'DELETED'")
public class CuttingAllocation extends FilialScopedEntity {

    public enum Source {
        SHEET,
        REMNANT
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false)
    private SaleOrder saleOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_item_id", nullable = false)
    private SaleOrderItem saleOrderItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remnant_id")
    private GlassRemnant remnant;

    /** Butun list qaysi oyna mahsulotidan kesilgani. Bo'sh bo'lsa — qatorning o'zi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_goods_id")
    private Goods sheetGoods;

    /** Shu manbadan olinadigan buyurtma donalari. */
    @Column(nullable = false)
    private Integer quantity;
}
