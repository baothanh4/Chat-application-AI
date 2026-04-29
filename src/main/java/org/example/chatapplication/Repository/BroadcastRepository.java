package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BroadcastRepository extends JpaRepository<Broadcast, UUID> {
    List<Broadcast> findAllByOrderByCreatedAtDesc();
}

