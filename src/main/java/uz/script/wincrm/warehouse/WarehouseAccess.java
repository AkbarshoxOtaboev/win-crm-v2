package uz.script.wincrm.warehouse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.Status;

import java.util.List;
import java.util.Set;

/**
 * Sotuvchi faqat o‘ziga biriktirilgan omborlardan sotadi.
 * SUPER_ADMIN / ADMIN / DIRECTOR — cheklov yo‘q.
 * Boshqa rollar (SELLER emas) — ham cheklov yo‘q.
 */
@Component
public class WarehouseAccess {

    private static final Set<String> UNRESTRICTED_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");

    /**
     * Auth/FE uchun: {@code null} — cheklov yo‘q; ro‘yxat — ruxsat etilgan omborlar
     * (bo‘sh = hech qaysi ombordan sota olmaydi).
     */
    public List<Long> authWarehouseIds(User user) {
        if (user == null || !isSellerRestricted(user)) {
            return null;
        }
        if (user.getWarehouses() == null || user.getWarehouses().isEmpty()) {
            return List.of();
        }
        return user.getWarehouses().stream()
                .filter(w -> w.getStatus() == null || w.getStatus() != Status.DELETED)
                .map(Warehouse::getId)
                .toList();
    }

    public void assertWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return;
        }
        User user = currentUser();
        if (user == null || !isSellerRestricted(user)) {
            return;
        }
        List<Long> allowed = authWarehouseIds(user);
        if (allowed == null || !allowed.contains(warehouseId)) {
            throw new ForbiddenException("Siz faqat o‘zingizga biriktirilgan ombordan sotishingiz mumkin");
        }
    }

    private boolean isSellerRestricted(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        if (user.getRoles().stream().anyMatch(r -> UNRESTRICTED_ROLES.contains(r.getName()))) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r -> "SELLER".equals(r.getName()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CustomUserDetails details
                ? details.getUser()
                : null;
    }
}
