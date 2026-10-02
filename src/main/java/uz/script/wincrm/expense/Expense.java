package uz.script.wincrm.expense;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = TableName.EXPENSE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("status <> 'DELETED'")
public class Expense extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    /**
     * Yetkazib beruvchiga to'lovdan avtomatik yaratilgan xarajat bo'lsa - o'sha to'lov ID'si.
     * Bunday xarajat faqat yetkazib beruvchi to'lovi orqali o'zgaradi/o'chiriladi.
     */
    @Column(name = "supplier_payment_id")
    private Long supplierPaymentId;
}