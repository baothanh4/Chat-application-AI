package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    List<UserRoleAssignment> findByUserId(UUID userId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}

