package live.toon.api.dto;

import lombok.Data;

/**
 * Corps d'une requête de mise à jour du profil utilisateur.
 * Seuls les champs non-null sont appliqués.
 */
@Data
public class UserUpdateRequest {
    private String email;
    private String gender;
}
