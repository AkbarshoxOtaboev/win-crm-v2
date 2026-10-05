package uz.script.wincrm.currency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;

/** Kompaniyaning o'zi belgilagan valyuta olish/sotish kursi (1 birlik valyuta = so'm); har valyuta uchun bitta qator. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(
        name = TableName.COMPANY_FX_RATES,
        uniqueConstraints = @UniqueConstraint(name = "uk_company_fx_rate_currency", columnNames = {"currency"})
)
public class CompanyFxRate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "buy_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal buyRate;

    @Column(name = "sell_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal sellRate;

    @Column(name = "updated_username")
    private String updatedUsername;
}
