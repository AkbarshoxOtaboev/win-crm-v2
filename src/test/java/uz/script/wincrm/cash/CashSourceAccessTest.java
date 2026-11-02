package uz.script.wincrm.cash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashSourceAccessTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cashierDefaultsToOperationalAndCannotUseVault() {
        authenticate(userWithRole("CASHIER"));
        assertEquals(CashSource.OPERATIONAL, CashSourceAccess.resolve(null));
        assertThrows(ForbiddenException.class, () -> CashSourceAccess.resolve(CashSource.VAULT));
    }

    @Test
    void adminDefaultsToVaultAndMayUseOperational() {
        authenticate(userWithRole("ADMIN"));
        assertEquals(CashSource.VAULT, CashSourceAccess.resolve(null));
        assertEquals(CashSource.OPERATIONAL, CashSourceAccess.resolve(CashSource.OPERATIONAL));
        assertEquals(CashSource.VAULT, CashSourceAccess.resolve(CashSource.VAULT));
    }

    @Test
    void directorCanSpendFromVault() {
        authenticate(userWithRole("DIRECTOR"));
        assertEquals(CashSource.VAULT, CashSourceAccess.resolve(CashSource.VAULT));
    }

    private static User userWithRole(String roleName) {
        Role role = Role.builder().name(roleName).status(Status.ACTIVE).permissions(Set.of()).build();
        return User.builder()
                .id(1L)
                .username("u")
                .password("x")
                .status(Status.ACTIVE)
                .roles(Set.of(role))
                .build();
    }

    private static void authenticate(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
