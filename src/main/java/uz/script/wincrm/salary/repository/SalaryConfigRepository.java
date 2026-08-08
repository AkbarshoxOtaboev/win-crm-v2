package uz.script.wincrm.salary.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.salary.SalaryConfig;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SalaryConfigRepository extends JpaRepository<SalaryConfig, Long> {

    Page<SalaryConfig> findByUserId(Long userId, Pageable pageable);

    /**
     * Hozir amaldagi (ochiq) konfiguratsiya: effectiveTo IS NULL bo'lgan eng so'nggi yozuv.
     * Bir user uchun ayni paytda faqat bitta ochiq konfiguratsiya bo'lishi kutiladi.
     */
    Optional<SalaryConfig> findFirstByUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(Long userId);

    /**
     * Berilgan sanada kuchda bo'lgan konfiguratsiya:
     * effectiveFrom <= date AND (effectiveTo IS NULL OR effectiveTo >= date).
     * Oylik slip hisoblashda o'sha davrga to'g'ri keladigan stavkani topish uchun ishlatiladi.
     */
    @Query("SELECT c FROM SalaryConfig c " +
            "WHERE c.user.id = :userId " +
            "AND c.effectiveFrom <= :date " +
            "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date) " +
            "ORDER BY c.effectiveFrom DESC")
    Optional<SalaryConfig> findEffectiveConfig(@Param("userId") Long userId,
                                               @Param("date") LocalDate date);
}
