package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByNameIgnoreCase(String name);

    @Query("SELECT r FROM Role r WHERE r.builtIn = :builtIn ORDER BY r.name")
    List<Role> findByBuiltInOrderByName(@Param("builtIn") boolean builtIn);
}

