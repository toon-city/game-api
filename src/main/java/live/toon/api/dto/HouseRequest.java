package live.toon.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import live.toon.api.entity.HouseAccess;
import lombok.Data;

@Data
public class HouseRequest {

    @NotBlank(message = "Le nom ne peut pas être vide")
    @Size(min = 2, max = 64, message = "Le nom doit contenir entre 2 et 64 caractères")
    private String name;

    /** ID du schéma de maison (gabarit). Obligatoire à la création. */
    private Long schemaId;

    /** Mode d'accès : OPEN, PASSWORD ou CLOSED */
    @NotNull(message = "Le mode d'accès est obligatoire")
    private HouseAccess access;

    /**
     * Mot de passe de la maison. Obligatoire si access = PASSWORD.
     * Non utilisé (ignoré) si access = OPEN ou CLOSED.
     */
    private String password;
}
