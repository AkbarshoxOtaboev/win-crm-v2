package uz.script.wincrm.finishedgoods;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.finishedgoods.enums.FinishedGoodsDocStatus;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.Warehouse;
import uz.script.wincrm.workshop.Workshop;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.FINISHED_GOODS_RECEIPTS)
public class FinishedGoodsReceipt extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_status", nullable = false, length = 20)
    private FinishedGoodsDocStatus docStatus;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @lombok.Builder.Default
    private List<FinishedGoodsReceiptItem> items = new ArrayList<>();
}
