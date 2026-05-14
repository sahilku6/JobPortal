package com.jobportal.auth.repository;

import com.jobportal.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByUuid(String uuid);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    Page<User> findByRole(User.Role role, Pageable pageable);

    @Query(value = """
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        AND (:role IS NULL OR u.role = :role)
        AND (:isActive IS NULL OR u.isActive = :isActive)
        """,
        countQuery = """
        SELECT COUNT(u) FROM User u
        WHERE (:keyword IS NULL OR (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        AND (:role IS NULL OR u.role = :role)
        AND (:isActive IS NULL OR u.isActive = :isActive)
        """)
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("role") User.Role role,
                           @Param("isActive") Boolean isActive,
                           Pageable pageable);

    long countByRole(User.Role role);

    // Phase 4: OAuth
    Optional<User> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    // Phase 4: single-session JTI update
    @Modifying
    @Query("UPDATE User u SET u.currentTokenJti = :jti WHERE u.id = :id")
    void updateTokenJti(@Param("id") Long id, @Param("jti") String jti);

    // Phase 3: feedback tracking (returns 1 if updated, 0 if already set)
    @Modifying
    @Query("UPDATE User u SET u.hasPostedFirstJob = true WHERE u.id = :id AND u.hasPostedFirstJob = false")
    int markFirstJobPosted(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.hasAppliedFirstJob = true WHERE u.id = :id AND u.hasAppliedFirstJob = false")
    int markFirstJobApplied(@Param("id") Long id);
}
