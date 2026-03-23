package live.toon.api.security.policy;

import live.toon.api.security.JwtPrincipal;

/**
 * Contrat générique pour les politiques d'accès aux ressources.
 * Chaque implémentation gère les règles d'une entité métier (House, User, etc.).
 *
 * @param <T> type de la ressource protégée
 */
public interface Policy<T> {

    default boolean canView(JwtPrincipal actor, T resource) {
        return false;
    }

    default boolean canCreate(JwtPrincipal actor) {
        return true;
    }

    default boolean canUpdate(JwtPrincipal actor, T resource) {
        return false;
    }

    default boolean canDelete(JwtPrincipal actor, T resource) {
        return false;
    }
}
