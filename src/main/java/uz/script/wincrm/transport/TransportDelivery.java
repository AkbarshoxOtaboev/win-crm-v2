package uz.script.wincrm.transport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.transport.enums.DeliveryStatus;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Sotuv buyurtmasini mijozga yetkazish. Sotuv buyurtmasi IN_DELIVERY ga o'tganda yaratiladi,
 * sotuv menejeri DELIVERED ni tasdiqlaganda CONFIRMED bo'ladi va ishchilarga oylik hisoblanadi.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.TRANSPORT_DELIVERIES)
public class TransportDelivery extends FilialScopedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false, unique = true)
    private SaleOrder saleOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private TransportDriver driver;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = TableName.TRANSPORT_DELIVERY_WORKERS,
            joinColumns = @JoinColumn(name = "delivery_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id")
    )
    @Builder.Default
    private Set<TransportWorker> workers = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_id")
    private User acceptedBy;

    private LocalDateTime departedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;

    private LocalDateTime cancelledAt;

    /** Tasdiqlash paytidagi ishchilar foizi (snapshot). */
    @Column(precision = 7, scale = 2)
    private BigDecimal salaryPercent;

    /** Tasdiqlash paytida ishchilarga hisoblangan jami summa. */
    private BigDecimal salaryTotal;
}
