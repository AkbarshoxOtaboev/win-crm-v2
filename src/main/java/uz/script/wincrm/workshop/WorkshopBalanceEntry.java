package uz.script.wincrm.workshop;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.production.ProductionAssignment;
import uz.script.wincrm.production.ProductionOrder;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.workshop.enums.WorkshopBalanceEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(
        name = TableName.WORKSHOP_BALANCE_ENTRIES,
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workshop_balance_assignment_event",
                columnNames = {"production_assignment_id", "event_type"}
        )
)
public class WorkshopBalanceEntry extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_assignment_id", nullable = false)
    private ProductionAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_order_id", nullable = false)
    private SaleOrder saleOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private WorkshopBalanceEventType eventType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal orderTotalSum;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal feePercent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
