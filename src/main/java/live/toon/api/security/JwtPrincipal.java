package live.toon.api.security;

import lombok.Data;

import java.util.UUID;

@Data
public class JwtPrincipal {
    private final UUID userId;
    private final String username;
}
