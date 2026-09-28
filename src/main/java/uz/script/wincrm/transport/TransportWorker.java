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

/** Yetkazishda ishtirok etadigan ishchi (yuk ortuvchi, o'rnatuvchi). Tizimga login qilmaydi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.TRANSPORT_WORKERS)
public class TransportWorker extends FilialScopedEntity {

    @Column(nullable = false)
    private String fullName;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String note;
}
