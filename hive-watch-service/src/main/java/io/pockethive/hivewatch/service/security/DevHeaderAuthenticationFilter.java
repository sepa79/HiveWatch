package io.pockethive.hivewatch.service.security;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import io.pockethive.hivewatch.service.users.UserAuthQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DevHeaderAuthenticationFilter extends OncePerRequestFilter {
    static final String USERNAME_HEADER = "X-HW-Username";

    private final UserAuthQueryService userAuthQueryService;

    public DevHeaderAuthenticationFilter(UserAuthQueryService userAuthQueryService) {
        this.userAuthQueryService = userAuthQueryService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = request.getHeader(USERNAME_HEADER);
        if (username == null || username.trim().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserAuthQueryService.UserWithRoles loaded = userAuthQueryService.loadActiveUserByUsernameOrThrow(username);
            Set<HiveWatchRole> roles = loaded.roles();
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList();
            HiveWatchPrincipal principal = new HiveWatchPrincipal(
                    loaded.user().getId(),
                    loaded.user().getUsername(),
                    loaded.user().getDisplayName(),
                    roles
            );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, "N/A", authorities)
            );
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

