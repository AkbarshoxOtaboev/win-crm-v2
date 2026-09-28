package uz.script.wincrm.production;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Sotuv menejeri belgilagan sexlar ketma-ketligi (masalan: 1 - oyna kesish, 2 - steklo paket).
     * Bo'sh bo'lsa - eski usul: keyingi sexni sex xodimi qo'lda tanlaydi.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = TableName.PRODUCTION_ORDER_ROUTE,
            joinColumns = @JoinColumn(name = "production_order_id"),
            inverseJoinColumns = @JoinColumn(name = "workshop_id")
    )
    @OrderColumn(name = "step_no")
    @Builder.Default
    private List<Workshop> route = new ArrayList<>();
}
