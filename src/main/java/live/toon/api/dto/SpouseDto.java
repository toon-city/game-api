package live.toon.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SpouseDto {
    private String username;
    /** MALE, FEMALE, NON_BINARY — peut être null */
    private String gender;
    private Integer skinColor;
    private Integer hairColor;
    /** spriteKey (catégorie) -> spritePath, même format que GET /api/inventory/equipped. */
    private Map<String, String> clothing;
}
