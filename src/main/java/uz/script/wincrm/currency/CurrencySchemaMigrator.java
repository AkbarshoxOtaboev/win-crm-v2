package uz.script.wincrm.currency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ddl-auto=update eski cheklovlarni olib tashlamaydi: supplier_balance(supplier_id) unique bo'lib qolsa,
 * ta'minotchiga ikkinchi valyutadagi balans qatorini ochib bo'lmaydi. Eski to'lovlarda applied_amount
 * bo'sh - ular so'mda bo'lgani uchun paid_summ bilan to'ldiriladi.
 */
@Component
@Order(0)
@Slf4j
public class CurrencySchemaMigrator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        dropSingleColumnSupplierBalanceUnique();
        int backfilled = entityManager.createNativeQuery(
                        "UPDATE supplier_payments SET applied_amount = paid_summ WHERE applied_amount IS NULL")
                .executeUpdate();
        if (backfilled > 0) {
            log.info("Backfilled supplier_payments.applied_amount for {} rows", backfilled);
        }
    }

    private void dropSingleColumnSupplierBalanceUnique() {
        List<?> names = entityManager.createNativeQuery(
                        "SELECT c.conname FROM pg_constraint c " +
                                "WHERE c.conrelid = to_regclass('supplier_balance') AND c.contype = 'u' " +
                                "AND pg_get_constraintdef(c.oid) = 'UNIQUE (supplier_id)'")
                .getResultList();
        for (Object name : names) {
            entityManager.createNativeQuery("ALTER TABLE supplier_balance DROP CONSTRAINT \"" + name + "\"")
                    .executeUpdate();
            log.info("Dropped legacy unique constraint {} on supplier_balance(supplier_id)", name);
        }
    }
}
