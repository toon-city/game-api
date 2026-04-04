package live.toon.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Niveaux de rang utilisateur mappés sur les rôles Spring Security.
 * Nommés en ROLE_* pour compatibilité directe avec hasRole() dans @PreAuthorize.
 */
public enum UserRank {

    ROLE_USER(0),
    ROLE_MODERATOR(1),
    ROLE_ADMIN(2);

    private final int level;

    UserRank(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /** Vrai si ce rang est au moins égal au rang minimum requis. */
    public boolean isAtLeast(UserRank min) {
        return this.level >= min.level;
    }

    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(name());
    }

    /** Convertit le champ {@code rank} entier de l'entité User en enum. Défaut : ROLE_USER. */
    public static UserRank fromRank(int rank) {
        if (rank >= ROLE_ADMIN.level)     return ROLE_ADMIN;
        if (rank >= ROLE_MODERATOR.level) return ROLE_MODERATOR;
        return ROLE_USER;
    }
}
