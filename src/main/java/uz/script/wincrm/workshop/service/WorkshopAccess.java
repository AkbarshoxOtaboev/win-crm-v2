package uz.script.wincrm.workshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.filial.FilialContext;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.users.User;
import uz.script.wincrm.workshop.repository.WorkshopRepository;

import java.util.List;
import java.util.Set;

/**
 * Sex boshlig'i (Workshop.manager) faqat o'z sexlari navbati, dashboardi va topshiriqlari bilan ishlaydi.
 * SUPER_ADMIN/ADMIN/DIRECTOR sex boshlig'i qilib belgilangan bo'lsa ham cheklanmaydi.
 */
@Component
@RequiredArgsConstructor
public class WorkshopAccess {

    private static final Set<String> UNRESTRICTED_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "DIRECTOR");

    private final WorkshopRepository workshopRepository;

    /** Foydalanuvchi boshqaradigan faol sexlar; bo'sh ro'yxat - cheklov yo'q. */
    public List<Long> restrictedWorkshopIds(User user) {
        if (user == null || isUnrestricted(user)) {
            return List.of();
        }
        return FilialContext.callAs(null, () -> workshopRepository.findActiveIdsByManagerId(user.getId()));
    }

    public void assertWorkshop(Long workshopId) {
        List<Long> allowed = restrictedWorkshopIds(currentUser());
        if (!allowed.isEmpty() && !allowed.contains(workshopId)) {
            throw new ForbiddenException("Siz faqat o'zingiz boshqaradigan sex buyurtmalari bilan ishlay olasiz");
        }
    }

    private boolean isUnrestricted(User user) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> UNRESTRICTED_ROLES.contains(r.getName()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CustomUserDetails details ? details.getUser() : null;
    }
}
