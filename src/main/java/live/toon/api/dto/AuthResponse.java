package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String userId;
    private String username;
    private String gender;
    private int rank;
    private int toonizLevel;
    private int kreds;
    private int pez;
    /** Couleur de peau de l'avatar (entier hex, ex : 0xf7ceaf). */
    private Integer skinColor;
}
