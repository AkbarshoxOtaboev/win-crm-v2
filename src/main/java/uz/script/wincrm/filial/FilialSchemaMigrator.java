package uz.script.wincrm.filial;

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
 * Drops global unique constraints on client/supplier phone and INN so the same
 * values can exist in different filials.
 */
@Component
@Order(0)
@Slf4j
public class FilialSchemaMigrator implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        dropConstraint("permissions", "permissions_resource_check");
        dropSingleColumnUniques("clients");
        dropSingleColumnUniques("suppliers");
    }

    private void dropConstraint(String table, String constraint) {
        entityManager.createNativeQuery("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint)
                .executeUpdate();
        log.info("Dropped constraint {}.{}", table, constraint);
    }

    private void dropSingleColumnUniques(String table) {
        @SuppressWarnings("unchecked")
        List<String> constraints = entityManager.createNativeQuery("""
                        SELECT c.conname
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE n.nspname = 'public'
                          AND t.relname = :table
                          AND c.contype = 'u'
                          AND array_length(c.conkey, 1) = 1
                        """)
                .setParameter("table", table)
                .getResultList();

        for (String name : constraints) {
            entityManager.createNativeQuery("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name)
                    .executeUpdate();
            log.info("Dropped unique constraint {}.{}", table, name);
        }

        @SuppressWarnings("unchecked")
        List<String> indexes = entityManager.createNativeQuery("""
                        SELECT i.relname
                        FROM pg_index x
                        JOIN pg_class i ON i.oid = x.indexrelid
                        JOIN pg_class t ON t.oid = x.indrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE n.nspname = 'public'
                          AND t.relname = :table
                          AND x.indisunique
                          AND NOT x.indisprimary
                          AND x.indnatts = 1
                          AND NOT EXISTS (
                              SELECT 1 FROM pg_constraint c WHERE c.conindid = x.indexrelid
                          )
                        """)
                .setParameter("table", table)
                .getResultList();

        for (String name : indexes) {
            entityManager.createNativeQuery("DROP INDEX IF EXISTS " + name).executeUpdate();
            log.info("Dropped unique index {}.{}", table, name);
        }
    }
}
