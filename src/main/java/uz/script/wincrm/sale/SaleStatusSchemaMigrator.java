package uz.script.wincrm.sale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate's ddl-auto=update never refreshes enum CHECK constraints, so new
 * SalesOrderStatus values (e.g. READY) would be rejected by existing databases.
 */
@Component
@Order(0)
@Slf4j
public class SaleStatusSchemaMigrator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        dropConstraint("sale_orders", "sale_orders_sales_order_status_check");
        dropConstraint("sale_order_history", "sale_order_history_from_status_check");
        dropConstraint("sale_order_history", "sale_order_history_to_status_check");
        clearCancelledOrderDebts();
    }

    private void clearCancelledOrderDebts() {
        int updated = entityManager.createNativeQuery(
                        "UPDATE sale_orders SET debt_sum = 0 WHERE sales_order_status = 'CANCELLED' AND debt_sum <> 0")
                .executeUpdate();
        if (updated > 0) {
            log.info("Cleared debt of {} cancelled sale orders", updated);
        }
    }

    private void dropConstraint(String table, String constraint) {
        entityManager.createNativeQuery("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint)
                .executeUpdate();
        log.info("Dropped constraint {}.{}", table, constraint);
    }
}
