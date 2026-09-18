package uz.script.wincrm.filial;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class FilialInterceptor implements HandlerInterceptor {

    private final FilialAccess filialAccess;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        filialAccess.bindRequest(request);
        Long filialId = FilialContext.getFilialId();
        if (filialId != null) {
            entityManager.unwrap(Session.class)
                    .enableFilter("filialFilter")
                    .setParameter("filialId", filialId);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        FilialContext.clear();
    }
}
