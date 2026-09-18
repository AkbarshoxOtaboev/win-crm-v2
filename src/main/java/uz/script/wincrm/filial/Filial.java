package uz.script.wincrm.filial;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

@Entity
@Table(name = TableName.FILIALS)
@SQLRestriction("status <> 'DELETED'")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Filial extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 32)
    private String phone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id", unique = true)
    private User director;
}
