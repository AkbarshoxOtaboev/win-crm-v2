package uz.script.wincrm.clients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.script.wincrm.clients.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByPhone(String phone);

    boolean existsByInn(String inn);

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.clientGroup ORDER BY c.id ASC")
    List<Client> findAllByOrderByIdAsc();

    /** Telegram bot registratsiyasida telefon raqami bo'yicha mijozni bog'lash uchun. */
    Optional<Client> findByPhone(String phone);

    /**
     * Format farqlarini e'tiborsiz qoldirib telefon bo'yicha qidiradi
     * (masalan {@code +998-(97)-221-88-96} ↔ {@code 998972218896}).
     */
    @Query(value = """
            SELECT * FROM clients c
            WHERE c.status <> 'DELETED'
              AND (
                regexp_replace(COALESCE(c.phone, ''), '[^0-9]', '', 'g') = :digits
                OR regexp_replace(COALESCE(c.additional_phone, ''), '[^0-9]', '', 'g') = :digits
                OR (
                  length(:digits) = 12 AND :digits LIKE '998%'
                  AND (
                    regexp_replace(COALESCE(c.phone, ''), '[^0-9]', '', 'g') = substring(:digits from 4)
                    OR regexp_replace(COALESCE(c.additional_phone, ''), '[^0-9]', '', 'g') = substring(:digits from 4)
                  )
                )
              )
            ORDER BY c.id
            LIMIT 1
            """, nativeQuery = true)
    Optional<Client> findByPhoneDigits(@Param("digits") String digits);
}