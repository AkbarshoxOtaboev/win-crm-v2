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

/** Yetkazib beruvchi: o'z haydovchimiz va uning mashinasi. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.TRANSPORT_DRIVERS)
public class TransportDriver extends FilialScopedEntity {

    @Column(nullable = false)
    private String fullName;

    private String phone;

    private String carModel;

    private String carNumber;

    @Column(columnDefinition = "TEXT")
    private String note;
}
