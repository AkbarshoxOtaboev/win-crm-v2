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
@FilterDef(
        name = "filialFilter",
        parameters = @ParamDef(name = "filialId", type = Long.class, resolver = CurrentFilialIdResolver.class),
        defaultCondition = "(:filialId = 0 or filial_id = :filialId)",
        autoEnabled = true,
        applyToLoadByKey = true
)
@Filter(name = "filialFilter")
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
            // Super admin sozlamalarda filial tanlamasdan ham yozishi mumkin
            if (FilialContext.isSuperAdmin()) {
                return;
            }
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
