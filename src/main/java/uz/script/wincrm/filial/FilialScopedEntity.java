package uz.script.wincrm.filial;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.utils.BaseEntity;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "filialFilter", parameters = @ParamDef(name = "filialId", type = Long.class))
@Filter(name = "filialFilter", condition = "filial_id = :filialId")
public abstract class FilialScopedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filial_id")
    private Filial filial;

    @PrePersist
    protected void assignFilialOnCreate() {
        if (this.filial != null) {
            return;
        }
        if (!FilialContext.isBound()) {
            return;
        }
        Long filialId = FilialContext.getFilialId();
        if (filialId == null) {
            throw new BadRequestException("Filial tanlanmagan. Avval filialni tanlang.");
        }
        if (filialId <= 0) {
            throw new ForbiddenException("Sizga filial biriktirilmagan");
        }
        Filial ref = new Filial();
        ref.setId(filialId);
        this.filial = ref;
    }
}
