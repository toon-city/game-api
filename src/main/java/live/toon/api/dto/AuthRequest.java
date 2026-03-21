package live.toon.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Le pseudo ne peut pas être vide")
    @Size(min = 2, max = 32, message = "Le pseudo doit contenir entre 2 et 32 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Le pseudo ne peut contenir que des lettres, chiffres, _ et -")
    private String username;

    private String password;

    /** Optionnel à l'inscription : MALE, FEMALE, NON_BINARY */
    private String gender;

    @Email(message = "L'adresse email est invalide")
    private String email;
}
