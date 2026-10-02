package uz.script.wincrm.kpi;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;

/** Xodim yakunlangan buyurtmadan oladigan KPI foizi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.KPI_RATES)
public class KpiRate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;
}
