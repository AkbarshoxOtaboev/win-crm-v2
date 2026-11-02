package uz.script.wincrm.cash;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.security.CustomUserDetails;

import java.util.Set;

/**
 * Kassir faqat Operatsion, ADMIN/DIRECTOR/SUPER_ADMIN Rahbar (VAULT) kassasidan yozadi.
 */
public final class CashSourceAccess {

    private static final Set<String> VAULT_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");

    private CashSourceAccess() {
    }

    public static boolean canSpendFromVault(CustomUserDetails user) {
        if (user == null || user.getUser() == null || user.getUser().getRoles() == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        return user.getUser().getRoles().stream().anyMatch(r -> VAULT_ROLES.contains(r.getName()));
    }

    public static boolean canSpendFromVault() {
        return canSpendFromVault(currentDetailsOrNull());
    }

    /** Null yoki bo'sh bo'lsa — roldan default. */
    public static CashSource resolve(CashSource requested) {
        CustomUserDetails user = currentDetails();
        if (requested == null) {
            return canSpendFromVault(user) ? CashSource.VAULT : CashSource.OPERATIONAL;
        }
        if (requested == CashSource.VAULT && !canSpendFromVault(user)) {
            throw new ForbiddenException("Rahbar kassasidan faqat admin yoki direktor yozishi mumkin");
        }
        return requested;
    }

    public static CashSource require(CashSource requested) {
        if (requested == null) {
            throw new BadRequestException("Kassa manbai (cashSource) tanlanishi shart");
        }
        return resolve(requested);
    }

    private static CustomUserDetails currentDetails() {
        CustomUserDetails details = currentDetailsOrNull();
        if (details == null) {
            throw new ForbiddenException("Foydalanuvchi aniqlanmadi");
        }
        return details;
    }

    private static CustomUserDetails currentDetailsOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return details;
    }
}
