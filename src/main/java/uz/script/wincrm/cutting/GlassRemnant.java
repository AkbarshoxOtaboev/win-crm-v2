package uz.script.wincrm.cutting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.Warehouse;

import java.math.BigDecimal;

/** Kesishdan qolgan va qayta ishlatiladigan foydali bo‘lak. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.GLASS_REMNANTS)
@SQLRestriction("status <> 'DELETED'")
public class GlassRemnant extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal width;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal height;

    @Column(nullable = false)
    private Integer pieceCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_sale_order_id")
    private SaleOrder sourceSaleOrder;
}
