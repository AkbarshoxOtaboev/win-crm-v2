package uz.script.wincrm.clients;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.payment.Payment;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.suppliers.SupplierBalance;
import uz.script.wincrm.utils.TableName;

import java.util.List;

@Entity
@Table(
        name = TableName.CLIENTS,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_clients_phone_filial", columnNames = {"phone", "filial_id"}),
                @UniqueConstraint(name = "uk_clients_inn_filial", columnNames = {"inn", "filial_id"})
        }
)
@SQLRestriction("status <> 'DELETED'")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Client extends FilialScopedEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(length = 20)
    private String inn;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(length = 32)
    private String additionalPhone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 150)
    private String bankName;

    @Column(length = 10)
    private String mfo;

    @Column(length = 30)
    private String accountNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "client")
    private List<SaleOrder> saleOrders;

    @OneToMany(mappedBy = "client")
    private List<SaleOrderItem> saleOrderItems;

    @OneToMany(mappedBy = "client")
    private List<Payment> payments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_group_id")
    private ClientGroup clientGroup;

    @OneToOne(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private ClientBalance balance;
}