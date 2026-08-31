package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.iam.model.AppUser;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long>
{
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    // Los tres filtros son opcionales; un metodo derivado con tres nulos seria ilegible.
    @Query("""
           SELECT u FROM AppUser u
            WHERE (:role   IS NULL OR u.role   = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    Page<AppUser> search(@Param("role")   UserRole   role,
                         @Param("status") UserStatus status,
                         @Param("search") String     search,
                         Pageable pageable);
}
