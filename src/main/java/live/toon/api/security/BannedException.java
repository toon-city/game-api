package live.toon.api.security;

/** Exception lancée quand un utilisateur banni essaie de s'authentifier. */
public class BannedException extends RuntimeException {
    public BannedException(String reason) {
        super(reason);
    }
}
