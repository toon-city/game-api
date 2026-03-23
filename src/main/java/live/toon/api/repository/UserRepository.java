package live.toon.api.repository;

import live.toon.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    /** Recherche par pseudo — le tri est défini via le Pageable (Sort). */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    long countByOnlineTrue();
}
