package uz.script.wincrm.finishedgoods;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.FINISHED_GOODS_RECEIPT_ITEMS)
public class FinishedGoodsReceiptItem extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private FinishedGoodsReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal count;

    /** Ixtiyoriy tannarx (so'm / birlik). */
    @Column(name = "unit_cost", precision = 38, scale = 2)
    private BigDecimal unitCost;
}
