package live.toon.api.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Principal JWT enrichi avec les données fraîches de la DB.
 * Construit par {@link JwtAuthFilter} après chargement du User depuis UserRepository.
 */
@Data
public class JwtPrincipal {

    private final UUID     userId;
    private final String   username;
    private final UserRank rank;

    /** Authorities Spring Security dérivées du rang DB (ex: ROLE_MODERATOR). */
    public List<GrantedAuthority> getAuthorities() {
        return List.of(rank.toGrantedAuthority());
    }

    /** Vrai si le rang de l'acteur est au moins égal au rang minimum requis. */
    public boolean isAtLeast(UserRank min) {
        return rank.isAtLeast(min);
    }

    public boolean isAdmin() {
        return rank.isAtLeast(UserRank.ROLE_ADMIN);
    }

    public boolean isModerator() {
        return rank.isAtLeast(UserRank.ROLE_MODERATOR);
    }
}
