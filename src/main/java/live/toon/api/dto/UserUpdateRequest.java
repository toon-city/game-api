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
    /** Free-text profile bio. Unlike email/gender, applied even if blank — clearing the bio is a valid edit. */
    private String description;
    /** Free-text profile title (e.g. "Pêcheur"). Same blank-clears-it rule as description. */
    private String job;
}
