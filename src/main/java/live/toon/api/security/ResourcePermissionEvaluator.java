package live.toon.api.security;

import live.toon.api.repository.RoomRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.policy.HousePolicy;
import live.toon.api.security.policy.UserPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Évaluateur central de permissions pour les annotations @PreAuthorize("hasPermission(...)").
 *
 * Dispatche par type de ressource vers la policy correspondante.
 * Les admins ({@link UserRank#ROLE_ADMIN}) bénéficient d'un bypass global avant tout dispatch.
 *
 * Usage dans les controllers :
 * <pre>
 *   @PreAuthorize("hasPermission(#id, 'House', 'update')")
 *   @PreAuthorize("hasPermission(#id, 'User',  'update')")
 * </pre>
 *
 * Types supportés : "House", "User"
 * Permissions supportées : "update", "delete"
 */
@Component
@RequiredArgsConstructor
public class ResourcePermissionEvaluator implements PermissionEvaluator {

    private final HousePolicy housePolicy;
    private final UserPolicy  userPolicy;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // Non utilisé : on passe toujours par targetId + targetType
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId,
                                 String targetType,
                                 Object permission) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof JwtPrincipal actor)) {
            return false;
        }

        // Bypass global admin : accès à toutes les ressources sans charger l'objet
        if (actor.isAdmin()) {
            return true;
        }

        String perm = permission.toString();
        return switch (targetType) {
            case "House" -> evaluateHousePermission(actor, (Long) targetId,   perm);
            case "User"  -> evaluateUserPermission (actor, (UUID) targetId,   perm);
            default      -> false;
        };
    }

    private boolean evaluateHousePermission(JwtPrincipal actor, Long roomId, String perm) {
        return roomRepository.findById(roomId)
                .map(room -> switch (perm) {
                    case "update" -> housePolicy.canUpdate(actor, room);
                    case "delete" -> housePolicy.canDelete(actor, room);
                    default       -> false;
                })
                .orElse(false);
    }

    private boolean evaluateUserPermission(JwtPrincipal actor, UUID userId, String perm) {
        return userRepository.findById(userId)
                .map(user -> switch (perm) {
                    case "update" -> userPolicy.canUpdate(actor, user);
                    case "delete" -> userPolicy.canDelete(actor, user);
                    default       -> false;
                })
                .orElse(false);
    }
}
