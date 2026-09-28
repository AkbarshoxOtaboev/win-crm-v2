package uz.script.wincrm.transport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;

/** Filial bo'yicha transport sozlamasi (bitta filialga bitta yozuv). */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.TRANSPORT_SETTINGS)
public class TransportSetting extends FilialScopedEntity {

    /** Buyurtma summasidan ishchilarga ajratiladigan foiz; biriktirilgan ishchilar o'rtasida teng bo'linadi. */
    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal workerSalaryPercent;
}
