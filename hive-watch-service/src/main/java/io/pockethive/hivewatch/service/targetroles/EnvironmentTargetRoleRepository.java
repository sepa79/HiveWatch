package io.pockethive.hivewatch.service.targetroles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentTargetRoleRepository extends JpaRepository<EnvironmentTargetRoleEntity, UUID> {
    List<EnvironmentTargetRoleEntity> findByEnvironmentId(UUID environmentId);

    List<EnvironmentTargetRoleEntity> findByEnvironmentIdIn(List<UUID> environmentIds);

    Optional<EnvironmentTargetRoleEntity> findByEnvironmentIdAndCode(UUID environmentId, String code);

    void deleteByEnvironmentId(UUID environmentId);
}
