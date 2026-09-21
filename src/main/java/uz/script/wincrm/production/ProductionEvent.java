package uz.script.wincrm.production;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.production.enums.ProductionEventType;
import uz.script.wincrm.users.User;
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
@Table(name = TableName.PRODUCTION_EVENTS)
public class ProductionEvent extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_workshop_id")
    private Workshop fromWorkshop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_workshop_id")
    private Workshop toWorkshop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
