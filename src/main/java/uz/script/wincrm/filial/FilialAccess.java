package uz.script.wincrm.filial;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ForbiddenException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;
import uz.script.wincrm.security.CustomUserDetails;
import uz.script.wincrm.utils.Status;

@Component
@RequiredArgsConstructor
public class FilialAccess {

    public static final String FILIAL_HEADER = "X-Filial-Id";

    private final FilialRepository filialRepository;

    public void bindRequest(HttpServletRequest request) {
        CustomUserDetails user = currentUser();
        if (user == null) {
            return;
        }
        if (user.isSuperAdmin()) {
            FilialContext.bind(parseHeader(request.getHeader(FILIAL_HEADER)), true);
            return;
        }
        Long assigned = user.getFilialId();
        FilialContext.bind(assigned != null ? assigned : -1L, false);
    }

    public CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            return null;
        }
        return user;
    }

    public boolean isSuperAdmin() {
        CustomUserDetails user = currentUser();
        return user != null && user.isSuperAdmin();
    }

    public Long currentFilialId() {
        return FilialContext.getFilialId();
    }

    public Filial requireCurrentFilial() {
        Long filialId = FilialContext.getFilialId();
        if (filialId == null) {
            throw new BadRequestException("Filial tanlanmagan. Super admin avval filialni tanlashi kerak.");
        }
        if (filialId <= 0) {
            throw new ForbiddenException("Sizga filial biriktirilmagan");
        }
        return filialRepository.findByIdAndStatusNot(filialId, Status.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Filial not found with id: " + filialId));
    }

    public void attachCurrentFilial(FilialScopedEntity entity) {
        if (entity == null || entity.getFilial() != null) {
            return;
        }
        entity.setFilial(requireCurrentFilial());
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ForbiddenException("Faqat super admin filial boshqara oladi");
        }
    }

    private Long parseHeader(String header) {
        if (header == null || header.isBlank() || "all".equalsIgnoreCase(header.trim())) {
            return null;
        }
        try {
            long id = Long.parseLong(header.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Noto'g'ri X-Filial-Id");
        }
    }
}
