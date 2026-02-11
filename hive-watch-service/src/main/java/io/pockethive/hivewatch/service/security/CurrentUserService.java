package io.pockethive.hivewatch.service.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUserService {
    public Optional<HiveWatchPrincipal> getOptionalPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof HiveWatchPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public HiveWatchPrincipal requirePrincipal() {
        return getOptionalPrincipal().orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authentication required"));
    }
}

