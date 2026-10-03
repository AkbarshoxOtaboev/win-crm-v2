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
 * ddl-auto=update eski cheklovlarni olib tashlamaydi: supplier_balance(supplier_id) yoki
 * client_balances(client_id) unique bo'lib qolsa, ikkinchi valyutadagi balans qatorini ochib bo'lmaydi.
 * Eski to'lovlarda applied_amount bo'sh - ular so'mda bo'lgani uchun to'langan summa bilan to'ldiriladi.
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
        dropSingleColumnUnique("supplier_balance", "supplier_id");
        dropSingleColumnUnique("client_balances", "client_id");
        backfill("UPDATE supplier_payments SET applied_amount = paid_summ WHERE applied_amount IS NULL",
                "supplier_payments");
        backfill("UPDATE payments SET applied_amount = payment_amount WHERE applied_amount IS NULL",
                "payments");
    }

    private void backfill(String sql, String table) {
        int rows = entityManager.createNativeQuery(sql).executeUpdate();
        if (rows > 0) {
            log.info("Backfilled {}.applied_amount for {} rows", table, rows);
        }
    }

    private void dropSingleColumnUnique(String table, String column) {
        List<?> names = entityManager.createNativeQuery(
                        "SELECT c.conname FROM pg_constraint c " +
                                "WHERE c.conrelid = to_regclass('" + table + "') AND c.contype = 'u' " +
                                "AND pg_get_constraintdef(c.oid) = 'UNIQUE (" + column + ")'")
                .getResultList();
        for (Object name : names) {
            entityManager.createNativeQuery("ALTER TABLE " + table + " DROP CONSTRAINT \"" + name + "\"")
                    .executeUpdate();
            log.info("Dropped legacy unique constraint {} on {}({})", name, table, column);
        }
    }
}
