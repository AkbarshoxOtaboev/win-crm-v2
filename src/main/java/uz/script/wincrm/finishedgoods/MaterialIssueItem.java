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
@Table(name = TableName.MATERIAL_ISSUE_ITEMS)
public class MaterialIssueItem extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_issue_id", nullable = false)
    private MaterialIssue materialIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    /** Ombor hisobi: PRODUCT — dona; WINDOW — kv.m. */
    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal count;

    /** WINDOW uchun dona (ixtiyoriy). */
    @Column(name = "piece_count", precision = 38, scale = 4)
    private BigDecimal pieceCount;
}
