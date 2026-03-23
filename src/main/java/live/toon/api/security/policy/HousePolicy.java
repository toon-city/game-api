package live.toon.api.security.policy;

import live.toon.api.entity.Room;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.UserRank;
import org.springframework.stereotype.Component;

/**
 * Politique d'accès aux maisons privées (Room de type PRIVATE).
 *
 * Règles :
 * - canUpdate / canDelete : propriétaire OU admin
 * - canBypassRestrictions : propriétaire, modérateur OU admin
 *     → utilisé par HouseService.validateAccess() pour ignorer accès CLOSED / PASSWORD
 */
@Component
public class HousePolicy implements Policy<Room> {

    @Override
    public boolean canView(JwtPrincipal actor, Room room) {
        return true; // La liste des maisons est publique
    }

    @Override
    public boolean canCreate(JwtPrincipal actor) {
        return true; // Tout utilisateur authentifié peut créer une maison
    }

    /** Propriétaire ou admin peuvent modifier la maison. */
    @Override
    public boolean canUpdate(JwtPrincipal actor, Room room) {
        return isOwner(actor, room) || actor.isAdmin();
    }

    /** Propriétaire ou admin peuvent supprimer la maison. */
    @Override
    public boolean canDelete(JwtPrincipal actor, Room room) {
        return isOwner(actor, room) || actor.isAdmin();
    }

    /**
     * Les modérateurs et admins passent les restrictions d'entrée (CLOSED, PASSWORD)
     * sans avoir à fournir de mot de passe. Le propriétaire aussi.
     */
    public boolean canBypassRestrictions(JwtPrincipal actor, Room room) {
        return isOwner(actor, room) || actor.isAtLeast(UserRank.ROLE_MODERATOR);
    }

    private boolean isOwner(JwtPrincipal actor, Room room) {
        return room.getOwner() != null && room.getOwner().getId().equals(actor.getUserId());
    }
}
