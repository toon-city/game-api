package live.toon.api.security.policy;

import live.toon.api.entity.User;
import live.toon.api.security.JwtPrincipal;
import org.springframework.stereotype.Component;

/**
 * Politique d'accès aux profils utilisateur.
 *
 * Règles :
 * - canUpdate : soi-même ou admin
 * - canDelete : admin uniquement
 */
@Component
public class UserPolicy implements Policy<User> {

    /** L'utilisateur peut modifier son propre profil ; l'admin peut modifier n'importe qui. */
    @Override
    public boolean canUpdate(JwtPrincipal actor, User user) {
        return isSelf(actor, user) || actor.isAdmin();
    }

    /** Seul un admin peut supprimer un compte. */
    @Override
    public boolean canDelete(JwtPrincipal actor, User user) {
        return actor.isAdmin();
    }

    private boolean isSelf(JwtPrincipal actor, User user) {
        return actor.getUserId().equals(user.getId());
    }
}
