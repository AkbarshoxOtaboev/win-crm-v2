package uz.script.wincrm.expense;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ddl-auto=update mavjud ustunning NOT NULL cheklovini yechmaydi.
 * Taqsimot endi bitta to'lovga emas, kirim + tur + valyuta guruhiga bog'lanadi.
 */
@Component
@Order(0)
@Slf4j
public class ImportExpenseSchemaMigrator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        dropNotNull("import_expenses", "warehouse_order_item_id");
        dropNotNull("import_expenses", "position_goods_name");
        dropNotNull("import_expense_allocations", "import_expense_id");
        backfillAllocationsFromLegacyItem();
        backfillAllocationGroups();
    }

    private void dropNotNull(String table, String column) {
        java.util.List<?> rows = entityManager.createNativeQuery(
                        "SELECT is_nullable FROM information_schema.columns " +
                                "WHERE table_schema = current_schema() AND table_name = :t AND column_name = :c")
                .setParameter("t", table)
                .setParameter("c", column)
                .getResultList();
        Object nullable = rows.isEmpty() ? null : rows.get(0);
        if (!"NO".equals(nullable)) {
            return;
        }
        entityManager.createNativeQuery("ALTER TABLE " + table + " ALTER COLUMN " + column + " DROP NOT NULL")
                .executeUpdate();
        log.info("Dropped NOT NULL on {}.{}", table, column);
    }

    /** Pozitsiyaga yozilgan eski xarajatni bitta ulush qilib ko'chiradi. */
    private void backfillAllocationsFromLegacyItem() {
        if (!tableExists("import_expense_allocations")) {
            return;
        }
        int rows = entityManager.createNativeQuery("""
                INSERT INTO import_expense_allocations (
                    import_expense_id, warehouse_order_id, kind, currency,
                    warehouse_order_item_id, amount,
                    position_goods_name, position_width, position_height, position_pieces, position_qty,
                    status, filial_id, created_at, updated_at
                )
                SELECT e.id, e.warehouse_order_id, e.kind, coalesce(e.currency, 'UZS'),
                       e.warehouse_order_item_id, e.amount,
                       coalesce(e.position_goods_name, ''), e.position_width, e.position_height,
                       e.position_pieces, e.position_qty,
                       'ACTIVE', e.filial_id, now(), now()
                FROM import_expenses e
                WHERE e.warehouse_order_item_id IS NOT NULL
                  AND e.status <> 'DELETED'
                  AND NOT EXISTS (
                      SELECT 1 FROM import_expense_allocations a
                      WHERE a.import_expense_id = e.id AND a.status <> 'DELETED'
                  )
                """).executeUpdate();
        if (rows > 0) {
            log.info("Backfilled {} legacy import expense allocations", rows);
        }
    }

    /** Eski taqsimotlarga order/kind/currency to'ldiradi. */
    private void backfillAllocationGroups() {
        if (!columnExists("import_expense_allocations", "warehouse_order_id")) {
            return;
        }
        int rows = entityManager.createNativeQuery("""
                UPDATE import_expense_allocations a
                SET warehouse_order_id = e.warehouse_order_id,
                    kind = e.kind,
                    currency = coalesce(e.currency, 'UZS')
                FROM import_expenses e
                WHERE a.import_expense_id = e.id
                  AND (a.warehouse_order_id IS NULL OR a.kind IS NULL OR a.currency IS NULL)
                """).executeUpdate();
        if (rows > 0) {
            log.info("Filled group keys on {} import expense allocations", rows);
        }
    }

    private boolean tableExists(String table) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM information_schema.tables " +
                                "WHERE table_schema = current_schema() AND table_name = :t")
                .setParameter("t", table)
                .getResultList()
                .isEmpty();
    }

    private boolean columnExists(String table, String column) {
        return !entityManager.createNativeQuery(
                        "SELECT 1 FROM information_schema.columns " +
                                "WHERE table_schema = current_schema() AND table_name = :t AND column_name = :c")
                .setParameter("t", table)
                .setParameter("c", column)
                .getResultList()
                .isEmpty();
    }
}
