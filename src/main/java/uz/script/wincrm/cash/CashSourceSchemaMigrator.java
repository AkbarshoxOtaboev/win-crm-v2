package uz.script.wincrm.cash;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eski xarajat/to'lovlar OPERATIONAL deb to'ldiriladi (ikkita kassa modeliga o'tish).
 */
@Component
@Order(0)
@Slf4j
public class CashSourceSchemaMigrator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        backfill("UPDATE expenses SET cash_source = 'OPERATIONAL' WHERE cash_source IS NULL", "expenses");
        backfill("UPDATE supplier_payments SET cash_source = 'OPERATIONAL' WHERE cash_source IS NULL",
                "supplier_payments");
    }

    private void backfill(String sql, String table) {
        try {
            int rows = entityManager.createNativeQuery(sql).executeUpdate();
            if (rows > 0) {
                log.info("Backfilled {}.cash_source → OPERATIONAL for {} rows", table, rows);
            }
        } catch (Exception e) {
            log.debug("Skip cash_source backfill on {}: {}", table, e.getMessage());
        }
    }
}
