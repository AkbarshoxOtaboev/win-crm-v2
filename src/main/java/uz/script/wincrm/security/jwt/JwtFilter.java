package uz.script.wincrm.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.script.wincrm.security.CustomUserDetailsService;
import uz.script.wincrm.security.blacklist.TokenBlacklistService;
import uz.script.wincrm.security.refreshToken.SessionService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            if (blacklistService.isBlacklisted(token)
                    || !jwtService.isTokenValid(token)
                    || !jwtService.isAccessToken(token)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            Long sid = jwtService.extractSid(token);

            if (sessionService.validateAndTouch(sid, resolveIp(request), request.getHeader("User-Agent")).isEmpty()) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user =
                        userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            // Do not abort the chain; permitAll endpoints can still proceed.
            // Authenticated API calls will get 401 from the entry point.
            org.slf4j.LoggerFactory.getLogger(JwtFilter.class)
                    .warn("JWT filter failed for {}: {}", request.getRequestURI(), e.toString());
            chain.doFilter(request, response);
        }
    }

    private String resolveIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
