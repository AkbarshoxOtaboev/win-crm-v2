package uz.script.wincrm.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.suppliers.SupplierPayment;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.utils.TableName;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.PAYMENT_TYPES)
@SQLRestriction("status <> 'DELETED'")
public class PaymentType extends FilialScopedEntity {
    @Column(nullable = false)
    private String name;

    @Column(length = 30)
    private String icon;

    /** Kassa valyutasi: shu turdagi to'lovlar shu valyutada qabul qilinadi. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency currency = Currency.UZS;

    @OneToMany(mappedBy = "paymentType")
    private List<Payment> payments;

    @OneToMany(mappedBy = "paymentType")
    private List<SupplierPayment> supplierPayments;
}
