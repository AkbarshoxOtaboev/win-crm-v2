package uz.script.wincrm.transport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.transport.enums.WorkerSalaryStatus;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitta yetkazish uchun bitta ishchiga hisoblangan oylik ulushi. Summa tasdiqlash paytidagi
 * qiymatlardan (buyurtma summasi, foiz, ishchilar soni) snapshot sifatida saqlanadi.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(
        name = TableName.TRANSPORT_WORKER_SALARIES,
        uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id", "worker_id"})
)
public class TransportWorkerSalary extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private TransportDelivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private TransportWorker worker;

    @Column(nullable = false)
    private Long saleOrderId;

    @Column(nullable = false)
    private BigDecimal orderTotalSnapshot;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal percentSnapshot;

    @Column(nullable = false)
    private Integer workersCount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerSalaryStatus salaryStatus;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    @Column(nullable = false)
    private Integer periodYear;

    @Column(nullable = false)
    private Integer periodMonth;

    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    private String comment;
}
