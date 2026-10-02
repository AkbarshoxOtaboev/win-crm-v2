package uz.script.wincrm.discount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;

/** Rol bo'yicha buyurtmaga beriladigan umumiy chegirmaning maksimal foizi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.DISCOUNT_RULES)
public class DiscountRule extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, unique = true)
    private Role role;

    /** null - cheklovsiz. */
    @Column(name = "max_discount_percent", precision = 5, scale = 2)
    private BigDecimal maxDiscountPercent;
}
