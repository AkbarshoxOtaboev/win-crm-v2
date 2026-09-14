package uz.script.wincrm.permissions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.utils.Action;
import uz.script.wincrm.utils.Resource;
import uz.script.wincrm.utils.Status;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionsDataLoader implements CommandLineRunner {

    private final PermissionsRepository permissionsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        purgeOrphanedPermissions();

        for (Resource resource : Resource.values()) {
            for (Action action : Action.values()) {

                boolean exists = permissionsRepository
                        .existsByResourceAndAction(resource, action);

                if (!exists) {
                    Permissions permission = Permissions.builder()
                            .resource(resource)
                            .action(action)
                            .name(resource.name() + "_" + action.name())
                            .status(Status.ACTIVE)
                            .build();

                    permissionsRepository.save(permission);
                }
            }
        }

        System.out.println("Permissions generated successfully ✅");
    }

    /**
     * Removes DB permission rows whose resource/action enums no longer exist in code.
     * Prevents Hibernate EnumType mapping failures on startup / role load.
     */
    private void purgeOrphanedPermissions() {
        String resources = Arrays.stream(Resource.values())
                .map(r -> "'" + r.name() + "'")
                .collect(Collectors.joining(","));
        String actions = Arrays.stream(Action.values())
                .map(a -> "'" + a.name() + "'")
                .collect(Collectors.joining(","));

        entityManager.createNativeQuery("""
                DELETE FROM role_permissions
                WHERE permission_id IN (
                    SELECT id FROM permissions
                    WHERE resource NOT IN (%s) OR action NOT IN (%s)
                )
                """.formatted(resources, actions)).executeUpdate();

        int removed = entityManager.createNativeQuery("""
                DELETE FROM permissions
                WHERE resource NOT IN (%s) OR action NOT IN (%s)
                """.formatted(resources, actions)).executeUpdate();

        if (removed > 0) {
            System.out.println("Removed " + removed + " orphaned permission(s) ✅");
        }
    }
}
