package live.toon.api.dto;

import lombok.Data;

@Data
public class EnterHouseRequest {
    /** Mot de passe fourni par le client. Peut être null si access != PASSWORD. */
    private String password;
}
