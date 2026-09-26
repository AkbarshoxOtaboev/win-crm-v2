package uz.script.wincrm.stock;

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
 * Hibernate's ddl-auto=update never changes the scale of existing numeric columns, so quantity
 * columns created as numeric(38,2) keep rounding kv.m (e.g. 1.1371 -> 1.14). This widens them to
 * scale 4 (1 sm² precision) and re-derives stocks.piece_count from count.
 */
@Component
@Order(0)
@Slf4j
public class QuantityScaleMigrator implements CommandLineRunner {

    private static final int SCALE = 4;

    private static final List<String[]> COLUMNS = List.of(
            new String[]{"stocks", "count"},
            new String[]{"stocks", "piece_count"},
            new String[]{"stock_histories", "count"},
            new String[]{"stock_histories", "balance_after"},
            new String[]{"sale_order_items", "count"},
            new String[]{"sale_order_items", "width"},
            new String[]{"sale_order_items", "height"},
            new String[]{"warehouse_order_items", "count"},
            new String[]{"warehouse_order_items", "piece_count"},
            new String[]{"warehouse_order_items", "weight"},
            new String[]{"warehouse_order_items", "height"}
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        for (String[] c : COLUMNS) {
            widen(c[0], c[1]);
        }
        syncPieceCounts();
    }

    private void widen(String table, String column) {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT numeric_scale FROM information_schema.columns " +
                                "WHERE table_schema = current_schema() AND table_name = :t AND column_name = :c")
                .setParameter("t", table)
                .setParameter("c", column)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return;
        }
        int scale = ((Number) rows.get(0)).intValue();
        if (scale >= SCALE) {
            return;
        }
        entityManager.createNativeQuery(
                        "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE numeric(38," + SCALE + ")")
                .executeUpdate();
        log.info("Widened {}.{} from scale {} to {}", table, column, scale, SCALE);
    }

    private void syncPieceCounts() {
        int windows = entityManager.createNativeQuery(
                        "UPDATE stocks s SET piece_count = round(s.count * 10000 / (g.width * g.height), " + SCALE + ") " +
                                "FROM goods g WHERE g.id = s.goods_id AND g.type = 'WINDOW' " +
                                "AND g.width > 0 AND g.height > 0 " +
                                "AND s.piece_count IS DISTINCT FROM round(s.count * 10000 / (g.width * g.height), " + SCALE + ")")
                .executeUpdate();
        int others = entityManager.createNativeQuery(
                        "UPDATE stocks s SET piece_count = s.count FROM goods g " +
                                "WHERE g.id = s.goods_id AND g.type <> 'WINDOW' AND s.piece_count IS DISTINCT FROM s.count")
                .executeUpdate();
        if (windows + others > 0) {
            log.info("Re-derived stocks.piece_count from count: {} window, {} other rows", windows, others);
        }
    }
}
