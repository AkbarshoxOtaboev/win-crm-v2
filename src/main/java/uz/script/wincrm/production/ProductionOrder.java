package uz.script.wincrm.production;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.production.enums.ProductionOrderStatus;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.workshop.Workshop;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.PRODUCTION_ORDERS)
public class ProductionOrder extends FilialScopedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false, unique = true)
    private SaleOrder saleOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionOrderStatus productionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_workshop_id")
    private Workshop currentWorkshop;

    private LocalDateTime startedAt;

    private LocalDateTime doneAt;

    @Column(columnDefinition = "TEXT")
    private String note;
}
