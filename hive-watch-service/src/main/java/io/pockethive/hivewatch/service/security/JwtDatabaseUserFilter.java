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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtDatabaseUserFilter extends OncePerRequestFilter {
    private final HiveWatchAuthProperties authProperties;
    private final UserAuthQueryService userAuthQueryService;

    public JwtDatabaseUserFilter(HiveWatchAuthProperties authProperties, UserAuthQueryService userAuthQueryService) {
        this.authProperties = authProperties;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            filterChain.doFilter(request, response);
            return;
        }

        String claim = authProperties.jwt() == null ? null : authProperties.jwt().usernameClaim();
        String username = jwtAuth.getToken().getClaimAsString(claim == null || claim.isBlank() ? "preferred_username" : claim);
        if (username == null || username.trim().isBlank()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"FORBIDDEN\"}");
            return;
        }

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

        filterChain.doFilter(request, response);
    }
}

