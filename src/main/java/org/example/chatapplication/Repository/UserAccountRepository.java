package org.example.chatapplication.Repository;

import org.example.chatapplication.Model.Entity.UserAccount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    List<UserAccount> findByFaceLoginEnabledTrueAndFaceTemplateHashIsNotNull();

    long countByAccountLockedTrue();

    long countByLastSeenAtAfter(Instant threshold);

    List<UserAccount> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username,
            String displayName,
            Pageable pageable
    );

    @Query("""
            select u from UserAccount u
            where lower(u.displayName) like lower(concat('%', :query, '%'))
               or lower(u.username) like lower(concat('%', :query, '%'))
            order by case
                when lower(u.displayName) = lower(:query) then 0
                when lower(u.username) = lower(:query) then 1
                else 2
            end, lower(u.displayName), lower(u.username)
            """)
    List<UserAccount> searchByDisplayNameOrUsername(@Param("query") String query, Pageable pageable);
}
