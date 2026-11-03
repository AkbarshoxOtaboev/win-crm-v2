package uz.script.wincrm.workshop;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.workshop.enums.WorkshopPayBasis;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
@Table(name = TableName.WORKSHOPS)
public class Workshop extends FilialScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /** PERCENT bo'lsa buyurtma summasining foizi. Boshqa asoslarda 0. */
    @Column(precision = 7, scale = 2)
    private BigDecimal feePercent;

    /** Ish haqi asosi. NULL eski yozuv: PERCENT. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkshopPayBasis payBasis;

    /** SQUARE_METER / PERIMETER / LENGTH uchun bir birlik stavkasi, so'm. */
    @Column(precision = 19, scale = 2)
    private BigDecimal pieceRate;
}
