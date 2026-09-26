package uz.script.wincrm.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.stock.enums.StockStatus;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.STOCKS)
@SQLRestriction("status <> 'DELETED'")
public class Stock extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal count;

    /**
     * Miqdor (dona). Hosila qiymat: WINDOW uchun count (kv.m) / list yuzasi, boshqa turlar uchun count.
     * Manba har doim count; bu ustun StockServiceImpl tomonidan count bilan birga yangilanadi.
     */
    @Column(precision = 38, scale = 4)
    private BigDecimal pieceCount;
}
