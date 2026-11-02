package uz.script.wincrm.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.script.wincrm.filial.Filial;
import uz.script.wincrm.filial.FilialRepository;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.roles.RoleRepository;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;
import uz.script.wincrm.utils.Status;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(3)
public class UserDataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FilialRepository filialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureAdmin();
        ensureCashier();
    }

    private void ensureAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

        userRepository.save(User.builder()
                .fullName("Super Admin")
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .status(Status.ACTIVE)
                .phone("+998-97-221-88-96")
                .roles(Set.of(superAdminRole))
                .build());

        System.out.println("Default SUPER_ADMIN created.");
    }

    /** Test/kassa foydalanuvchi: login kassa / 12345 */
    private void ensureCashier() {
        if (userRepository.existsByUsername("kassa")) {
            return;
        }
        Role cashierRole = roleRepository.findByName("CASHIER")
                .orElseThrow(() -> new RuntimeException("CASHIER role not found"));
        Filial filial = filialRepository.findAll().stream().findFirst().orElse(null);

        userRepository.save(User.builder()
                .fullName("Kassir")
                .username("kassa")
                .password(passwordEncoder.encode("12345"))
                .status(Status.ACTIVE)
                .phone("+998-90-000-00-00")
                .filial(filial)
                .roles(Set.of(cashierRole))
                .build());

        System.out.println("Default CASHIER user created (kassa / 12345).");
    }
}
