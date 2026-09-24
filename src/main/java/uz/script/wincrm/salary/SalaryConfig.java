package uz.script.wincrm.salary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.salary.enums.CommissionType;
import uz.script.wincrm.users.User;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.utils.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bitta xodim (User) uchun oylik konfiguratsiyasi: fiksa oylik va komissiya qoidasi.
 *
 * DIQQAT: konfiguratsiya VAQT BO'YICHA versiyalanadi (effective-dated). Stavka o'zgarsa
 * eski yozuv effectiveTo bilan yopiladi va yangi yozuv ochiladi - shu tufayli o'tgan
 * oylardagi hisob-kitob buzilmaydi. Bir user uchun ayni paytda faqat bitta ochiq
 * (effectiveTo IS NULL yoki bugungi kunni qamragan) konfiguratsiya bo'lishi kerak.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.SALARY_CONFIGS)
@SQLRestriction("status <> 'DELETED'")
public class SalaryConfig extends FilialScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Oylik fiksa (kafolatlangan qism). Komissiyasiz model uchun 0 berilishi mumkin. */
    @Column(nullable = false)
    private BigDecimal baseSalary;

    /** Komissiya turi: PERCENT (foiz) yoki FIXED (aniq summa). Admin belgilaydi. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionType commissionType;

    /**
     * PERCENT bo'lsa - foiz (masalan 2 => 2%).
     * FIXED bo'lsa   - har to'liq to'langan buyurtma uchun beriladigan aniq summa.
     * Komissiya kerak bo'lmasa 0 berilishi mumkin.
     */
    @Column(nullable = false)
    private BigDecimal commissionValue;

    /** Ushbu konfiguratsiya kuchga kiradigan sana (shu kundan boshlab amal qiladi). */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /** Konfiguratsiya amal qilishini to'xtatgan sana. NULL bo'lsa - hozir amaldagi (ochiq) konfiguratsiya. */
    private LocalDate effectiveTo;
}
