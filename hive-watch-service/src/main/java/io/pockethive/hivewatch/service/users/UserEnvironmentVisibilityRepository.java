package io.pockethive.hivewatch.service.users;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEnvironmentVisibilityRepository extends JpaRepository<UserEnvironmentVisibilityEntity, UUID> {
    List<UserEnvironmentVisibilityEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

