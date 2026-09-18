package uz.script.wincrm.roles;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.permissions.Permissions;
import uz.script.wincrm.permissions.PermissionsRepository;
import uz.script.wincrm.utils.Status;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(2)
public class RoleDataLoader implements CommandLineRunner {

    private final RoleRepository repository;
    private final PermissionsRepository permissionsRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        Set<Permissions> allPermissions = new HashSet<>(permissionsRepository.findAll());

        if (repository.count() == 0) {
            Role superAdmin = Role.builder()
                    .name("SUPER_ADMIN")
                    .status(Status.ACTIVE)
                    .permissions(allPermissions)
                    .build();

            Role admin = Role.builder()
                    .name("ADMIN")
                    .status(Status.ACTIVE)
                    .permissions(allPermissions)
                    .build();

            repository.save(superAdmin);
            repository.save(admin);
            repository.save(Role.builder()
                    .name("DIRECTOR")
                    .status(Status.ACTIVE)
                    .permissions(new HashSet<>(allPermissions))
                    .build());

            System.out.println("SUPER_ADMIN role created successfully ✅");
            return;
        }

        if (repository.findByName("DIRECTOR").isEmpty()) {
            repository.save(Role.builder()
                    .name("DIRECTOR")
                    .status(Status.ACTIVE)
                    .permissions(new HashSet<>(allPermissions))
                    .build());
            System.out.println("DIRECTOR role created successfully ✅");
        }

        // Keep SUPER_ADMIN in sync with newly seeded permissions after enum additions
        syncAllPermissions("SUPER_ADMIN", allPermissions);
        syncAllPermissions("ADMIN", allPermissions);
        syncAllPermissions("DIRECTOR", allPermissions);
    }

    private void syncAllPermissions(String roleName, Set<Permissions> allPermissions) {
        Optional<Role> roleOpt = repository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            return;
        }

        Role role = roleOpt.get();
        Set<Permissions> current = role.getPermissions();
        if (current == null) {
            role.setPermissions(new HashSet<>(allPermissions));
            repository.save(role);
            return;
        }

        if (current.size() != allPermissions.size() || !current.containsAll(allPermissions)) {
            current.clear();
            current.addAll(allPermissions);
            repository.save(role);
            System.out.println(roleName + " permissions synced ✅");
        }
    }
}
