package io.pockethive.hivewatch.service.users;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class UserAuthQueryService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public UserAuthQueryService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional(readOnly = true)
    public UserWithRoles loadActiveUserByUsernameOrThrow(String username) {
        if (username == null || username.trim().isBlank()) {
            throw new ResponseStatusException(FORBIDDEN, "User not found");
        }
        UserEntity user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "User not found"));
        if (!user.isActive()) {
            throw new ResponseStatusException(FORBIDDEN, "User disabled");
        }
        Set<HiveWatchRole> roles = EnumSet.noneOf(HiveWatchRole.class);
        for (UserRoleEntity role : userRoleRepository.findByUserId(user.getId())) {
            roles.add(role.getRole());
        }
        if (roles.isEmpty()) {
            throw new ResponseStatusException(FORBIDDEN, "User has no roles");
        }
        return new UserWithRoles(user, Set.copyOf(roles));
    }

    public record UserWithRoles(UserEntity user, Set<HiveWatchRole> roles) {
    }
}

