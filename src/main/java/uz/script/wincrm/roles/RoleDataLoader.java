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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(2)
public class RoleDataLoader implements CommandLineRunner {

    private final RoleRepository repository;
    private final PermissionsRepository permissionsRepository;

    /** Sotuvchi — CRM + ishlab chiqarishga yuborish */
    private static final Set<String> SELLER_PERMISSIONS = Set.of(
            "CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_EDIT",
            "CLIENT_GROUP_VIEW",
            "CLIENT_BALANCE_VIEW",
            "CLIENT_NOTE_VIEW", "CLIENT_NOTE_CREATE", "CLIENT_NOTE_EDIT",
            "SALE_ORDER_VIEW", "SALE_ORDER_CREATE", "SALE_ORDER_EDIT",
            "SALE_ORDER_ITEM_VIEW", "SALE_ORDER_ITEM_CREATE", "SALE_ORDER_ITEM_EDIT", "SALE_ORDER_ITEM_DELETE",
            "SALE_ORDER_WASTE_VIEW", "SALE_ORDER_WASTE_CREATE",
            "WAREHOUSE_VIEW", "STOCK_VIEW",
            "GOODS_VIEW", "GOODS_GROUP_VIEW", "UNIT_TYPE_VIEW",
            "PAYMENT_VIEW", "PAYMENT_CREATE", "PAYMENT_TYPE_VIEW",
            "WORKSHOP_VIEW",
            "PRODUCTION_ORDER_VIEW", "PRODUCTION_ORDER_CREATE",
            "TRANSPORT_DELIVERY_VIEW",
            "CASH_HANDOVER_VIEW", "CASH_HANDOVER_CREATE",
            "DASHBOARD_VIEW"
    );

    /** Transport bo'limi menejeri — yetkazishlar, haydovchilar, ishchilar va ularning oyligi (tasdiqlashsiz) */
    private static final Set<String> TRANSPORT_MANAGER_PERMISSIONS = Set.of(
            "TRANSPORT_DELIVERY_VIEW", "TRANSPORT_DELIVERY_EDIT",
            "TRANSPORT_DRIVER_VIEW", "TRANSPORT_DRIVER_CREATE", "TRANSPORT_DRIVER_EDIT", "TRANSPORT_DRIVER_DELETE",
            "TRANSPORT_WORKER_VIEW", "TRANSPORT_WORKER_CREATE", "TRANSPORT_WORKER_EDIT", "TRANSPORT_WORKER_DELETE",
            "TRANSPORT_SALARY_VIEW",
            "SALE_ORDER_VIEW", "SALE_ORDER_ITEM_VIEW",
            "CLIENT_VIEW"
    );

    /** Kassir — mijozlardan kelgan to'lovlarni kiritadi (mijoz balansi va buyurtmalarni faqat ko'radi) */
    private static final Set<String> CASHIER_PERMISSIONS = Set.of(
            "PAYMENT_VIEW", "PAYMENT_CREATE", "PAYMENT_TYPE_VIEW",
            "CLIENT_VIEW", "CLIENT_BALANCE_VIEW",
            "SALE_ORDER_VIEW",
            "CASH_HANDOVER_VIEW", "CASH_HANDOVER_CREATE"
    );

    /** Ishlab chiqarish boshlig‘i — sexlar + barcha production */
    private static final Set<String> PRODUCTION_MANAGER_PERMISSIONS = Set.of(
            "WORKSHOP_VIEW", "WORKSHOP_CREATE", "WORKSHOP_EDIT", "WORKSHOP_DELETE",
            "PRODUCTION_ORDER_VIEW", "PRODUCTION_ORDER_CREATE", "PRODUCTION_ORDER_EDIT", "PRODUCTION_ORDER_DELETE",
            "SALE_ORDER_VIEW",
            "SALE_ORDER_ITEM_VIEW",
            "SALE_ORDER_WASTE_VIEW", "SALE_ORDER_WASTE_CREATE", "SALE_ORDER_WASTE_EDIT",
            "GOODS_VIEW", "GOODS_GROUP_VIEW",
            "WAREHOUSE_VIEW", "STOCK_VIEW",
            "CLIENT_VIEW",
            "DASHBOARD_VIEW"
    );

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        Set<Permissions> allPermissions = new HashSet<>(permissionsRepository.findAll());
        Map<String, Permissions> byName = allPermissions.stream()
                .collect(Collectors.toMap(Permissions::getName, Function.identity(), (a, b) -> a));

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
        }

        if (repository.findByName("DIRECTOR").isEmpty()) {
            repository.save(Role.builder()
                    .name("DIRECTOR")
                    .status(Status.ACTIVE)
                    .permissions(new HashSet<>(allPermissions))
                    .build());
            System.out.println("DIRECTOR role created successfully ✅");
        }

        // Keep SUPER_ADMIN / ADMIN / DIRECTOR in sync with newly seeded permissions
        syncAllPermissions("SUPER_ADMIN", allPermissions);
        syncAllPermissions("ADMIN", allPermissions);
        syncAllPermissions("DIRECTOR", allPermissions);

        // Operational roles for CRM / ERP workshop flow
        ensureRoleWithPermissions("SELLER", SELLER_PERMISSIONS, byName);
        ensureRoleWithPermissions("PRODUCTION_MANAGER", PRODUCTION_MANAGER_PERMISSIONS, byName);
        ensureRoleWithPermissions("TRANSPORT_MANAGER", TRANSPORT_MANAGER_PERMISSIONS, byName);
        ensureRoleWithPermissions("CASHIER", CASHIER_PERMISSIONS, byName);
        retireRole("WORKSHOP_MANAGER");
    }

    /** Soft-delete legacy roles that are no longer used. */
    private void retireRole(String roleName) {
        repository.findByName(roleName).ifPresent(role -> {
            if (role.getStatus() != Status.DELETED) {
                role.setStatus(Status.DELETED);
                repository.save(role);
                System.out.println(roleName + " role retired (DELETED) ✅");
            }
        });
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

    private void ensureRoleWithPermissions(
            String roleName,
            Set<String> permissionNames,
            Map<String, Permissions> byName
    ) {
        Set<Permissions> desired = permissionNames.stream()
                .map(byName::get)
                .filter(p -> p != null)
                .collect(Collectors.toCollection(HashSet::new));

        Optional<Role> existing = repository.findByName(roleName);
        if (existing.isEmpty()) {
            repository.save(Role.builder()
                    .name(roleName)
                    .status(Status.ACTIVE)
                    .permissions(desired)
                    .build());
            System.out.println(roleName + " role created with " + desired.size() + " permissions ✅");
            return;
        }

        Role role = existing.get();
        Set<Permissions> current = role.getPermissions();
        if (current == null) {
            role.setPermissions(desired);
            repository.save(role);
            System.out.println(roleName + " permissions assigned ✅");
            return;
        }

        if (current.size() != desired.size() || !current.containsAll(desired)) {
            current.clear();
            current.addAll(desired);
            repository.save(role);
            System.out.println(roleName + " permissions synced (" + desired.size() + ") ✅");
        }
    }
}
