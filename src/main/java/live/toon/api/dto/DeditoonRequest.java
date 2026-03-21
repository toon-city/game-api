package live.toon.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeditoonRequest {
    @NotBlank
    @Size(min = 1, max = 150)
    private String message;
}
