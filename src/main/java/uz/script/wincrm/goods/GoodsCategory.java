package uz.script.wincrm.goods;

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

@Entity
@SQLRestriction("status <> 'DELETED'")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.GOODS_CATEGORY)
@Getter
@Setter
@SuperBuilder
public class GoodsCategory extends FilialScopedEntity {

    @Column(nullable = false, length = 100)
    private String name;
}
