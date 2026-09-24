package uz.script.wincrm.filial;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Catalog rows created before filial scoping have a null filial_id.
 * They are attached to the earliest active filial so a selected filial
 * does not show another filial's products, groups, or units.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilialCatalogBackfill implements ApplicationRunner {

    private static final String[] TABLES = {
            "goods",
            "goods_group",
            "unit_types",
            "client_groups",
            "expense_categories",
            "payment_types",
            "salary_configs",
            "salary_transactions"
    };

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            dropExpenseCategoryNameUnique();
            Long filialId = firstActiveFilialId();
            if (filialId == null) {
                return;
            }
            for (String table : TABLES) {
                if (!hasColumn(table, "filial_id")) {
                    continue;
                }
                int updated = jdbc.update(
                        "UPDATE " + table + " SET filial_id = ? WHERE filial_id IS NULL",
                        filialId
                );
                if (updated > 0) {
                    log.info("Assigned {} existing {} rows to filial {}", updated, table, filialId);
                }
            }
        } catch (Exception ex) {
            log.warn("Filial catalog backfill skipped: {}", ex.getMessage());
        }
    }

    private Long firstActiveFilialId() {
        return jdbc.query(
                "SELECT id FROM filials WHERE status <> 'DELETED' ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null
        );
    }

    private boolean hasColumn(String table, String column) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }

    private void dropExpenseCategoryNameUnique() {
        jdbc.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                  FOR r IN
                    SELECT c.conname
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = t.relnamespace
                    WHERE t.relname = 'expense_categories'
                      AND n.nspname = current_schema()
                      AND c.contype = 'u'
                      AND array_length(c.conkey, 1) = 1
                      AND EXISTS (
                        SELECT 1
                        FROM pg_attribute a
                        WHERE a.attrelid = t.oid
                          AND a.attnum = c.conkey[1]
                          AND a.attname = 'name'
                      )
                  LOOP
                    EXECUTE format('ALTER TABLE expense_categories DROP CONSTRAINT %I', r.conname);
                  END LOOP;
                END $$;
                """);
    }
}
