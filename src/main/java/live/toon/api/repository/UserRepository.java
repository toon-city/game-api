package live.toon.api.repository;

import live.toon.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    /**
     * Locked read — used by MarriageService.respond() to serialize concurrent
     * accept attempts on the same pair of users (same reasoning as
     * ShopItemRepository.findByIdForUpdate for stock races).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    /** Recherche par pseudo — le tri est défini via le Pageable (Sort). */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<User> findByBanned(boolean banned, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseAndBanned(String username, boolean banned, Pageable pageable);
    long countByOnlineTrue();
    long countByBannedTrue();
}
