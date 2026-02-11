package io.pockethive.hivewatch.service.users;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import io.pockethive.hivewatch.service.api.UserCreateRequestDto;
import io.pockethive.hivewatch.service.api.UserEnvironmentVisibilityUpdateRequestDto;
import io.pockethive.hivewatch.service.api.UserSummaryDto;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserAdminService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserEnvironmentVisibilityRepository userEnvironmentVisibilityRepository;

    public UserAdminService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            UserEnvironmentVisibilityRepository userEnvironmentVisibilityRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userEnvironmentVisibilityRepository = userEnvironmentVisibilityRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> listUsers() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        listRoles(user.getId()),
                        user.isActive()
                ))
                .toList();
    }

    @Transactional
    public UserSummaryDto createUser(UserCreateRequestDto request) {
        validateCreateRequest(request);
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "username already exists");
        }

        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        userRepository.save(new UserEntity(userId, username, request.displayName().trim(), request.active(), now));

        Set<HiveWatchRole> roles = EnumSet.copyOf(request.roles());
        for (HiveWatchRole role : roles) {
            userRoleRepository.save(new UserRoleEntity(UUID.randomUUID(), userId, role, now));
        }

        return new UserSummaryDto(userId, username, request.displayName().trim(), List.copyOf(roles), request.active());
    }

    @Transactional(readOnly = true)
    public UserEnvironmentVisibilityUpdateRequestDto getVisibility(UUID userId) {
        requireUser(userId);
        List<UUID> envIds = userEnvironmentVisibilityRepository.findByUserId(userId).stream()
                .map(UserEnvironmentVisibilityEntity::getEnvironmentId)
                .distinct()
                .sorted()
                .toList();
        return new UserEnvironmentVisibilityUpdateRequestDto(envIds);
    }

    @Transactional
    public UserEnvironmentVisibilityUpdateRequestDto replaceVisibility(UUID userId, UserEnvironmentVisibilityUpdateRequestDto request) {
        requireUser(userId);
        if (request == null || request.environmentIds() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "environmentIds is required");
        }
        if (request.environmentIds().stream().anyMatch(id -> id == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "environmentIds must not contain null");
        }

        userEnvironmentVisibilityRepository.deleteByUserId(userId);
        Instant now = Instant.now();
        for (UUID envId : request.environmentIds().stream().distinct().toList()) {
            userEnvironmentVisibilityRepository.save(new UserEnvironmentVisibilityEntity(UUID.randomUUID(), userId, envId, now));
        }
        return getVisibility(userId);
    }

    private List<HiveWatchRole> listRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRoleEntity::getRole)
                .distinct()
                .toList();
    }

    private void requireUser(UUID userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
    }

    private static void validateCreateRequest(UserCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.username() == null || request.username().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username is required");
        }
        if (!request.username().trim().matches("[a-zA-Z0-9._-]+")) {
            throw new ResponseStatusException(BAD_REQUEST, "username must match [a-zA-Z0-9._-]+");
        }
        if (request.displayName() == null || request.displayName().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "displayName is required");
        }
        if (request.roles() == null || request.roles().isEmpty() || request.roles().stream().anyMatch(r -> r == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "roles is required");
        }
    }
}

