package uz.script.wincrm.salary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.salary.enums.SalaryEntryType;
import uz.script.wincrm.utils.BaseEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Oylik ledger yozuvi - komissiya va qo'lda kiritilgan tuzatishlar (bonus/deduction/advance)
 * uchun O'ZGARMAS SNAPSHOT. Real vaqtda qayta hisoblanmaydi: yozilgan payt qiymatlari
 * (rateSnapshot, baseAmountSnapshot) shu yerda saqlanib qoladi. Bu sizning
 * SaleOrderDiscountHistory / cashback EARNED-REVERSED pattern'ingiz bilan bir xil yondashuv.
 *
 * Bog'lanishlar loose coupling orqali (plain Long id), ClientNote'dagidek - modullararo
 * qattiq FK bog'liqlikni oldini olish uchun.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.SALARY_TRANSACTIONS)
@SQLRestriction("status <> 'DELETED'")
public class SalaryTransaction extends BaseEntity {

    /** Komissiya/tuzatish tegishli bo'lgan xodim (sotuvchi) ID si. */
    @Column(nullable = false)
    private Long userId;

    /** Manba buyurtma ID si (komissiya yozuvlari uchun). Qo'lda tuzatishlarda NULL bo'lishi mumkin. */
    private Long saleOrderId;

    /** Yozuv turi (COMMISSION, COMMISSION_REVERSAL, BONUS, DEDUCTION, ADVANCE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryEntryType entryType;

    /** Summa - HAR DOIM musbat magnitud. Ishorasi entryType orqali qo'llaniladi. */
    @Column(nullable = false)
    private BigDecimal amount;

    /** Komissiya yozuvi paytidagi komissiya turi snapshoti (audit uchun). */
    @Enumerated(EnumType.STRING)
    private CommissionType commissionTypeSnapshot;

    /** Komissiya stavkasi snapshoti (PERCENT bo'lsa foiz, FIXED bo'lsa summa). */
    private BigDecimal rateSnapshot;

    /** Komissiya hisoblangan baza summasi snapshoti (odatda buyurtmaning to'langan qismi). */
    private BigDecimal baseAmountSnapshot;

    /** Yozuv tan olingan vaqt - shu vaqt asosida periodYear/periodMonth aniqlanadi. */
    @Column(nullable = false)
    private LocalDateTime earnedAt;

    /** Oylik hisob davri - yil (earnedAt asosida). Agregatsiya/indeks uchun denormalizatsiya. */
    @Column(nullable = false)
    private Integer periodYear;

    /** Oylik hisob davri - oy 1..12 (earnedAt asosida). */
    @Column(nullable = false)
    private Integer periodMonth;

    private String comment;
}
