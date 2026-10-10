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
        repairWindowItemCosts();
    }

    /**
     * Oyna pozitsiyalarida tannarx o'rniga sotish narxining so'mdagi qiymati yozilib qolgan
     * (price_cost = price_selling * kurs). Ularga ombordagi o'rtacha tannarx (bo'lmasa tovar tannarxi) qo'yiladi.
     */
    private void repairWindowItemCosts() {
        int updated = entityManager.createNativeQuery("""
                        UPDATE sale_order_items soi
                        SET price_cost = COALESCE(
                                (SELECT NULLIF(st.price_cost, 0) FROM stocks st
                                 WHERE st.goods_id = soi.goods_id AND st.warehouse_id = soi.warehouse_id
                                 LIMIT 1),
                                g.price_cost)
                        FROM goods g, sale_orders so
                        WHERE g.id = soi.goods_id
                          AND so.id = soi.sale_order_id
                          AND g.type = 'WINDOW'
                          AND g.price_cost > 0
                          AND soi.price_cost = ROUND(soi.price_selling * COALESCE(so.exchange_rate, 1), 2)
                        """)
                .executeUpdate();
        if (updated > 0) {
            log.info("Repaired cost price of {} window sale order items", updated);
        }
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
