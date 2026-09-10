package live.toon.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Talks to game-server-java's internal-only HTTP endpoints. The two
 * services otherwise never call each other directly — they share the same
 * Postgres DB and stay decoupled (game-server-java hand-mirrors the entities
 * it needs). This is the one deliberate exception: an immediate kick on ban
 * needs to reach a live in-memory STOMP session, which only game-server-java
 * has, and there is no other channel to signal it in real time.
 *
 * Every call is fire-and-forget from the caller's point of view: a failure
 * here (game-server-java down, network blip) must never fail the ban itself.
 * The ban is still fully enforced on the next login (AuthService) and STOMP
 * CONNECT (JwtChannelInterceptor) regardless of whether this call succeeds.
 */
@Slf4j
@Component
public class GameServerClient {

    private final RestClient restClient;
    private final String internalSecret;

    public GameServerClient(
            @Value("${game-server.internal-url}") String internalUrl,
            @Value("${game-server.internal-secret}") String internalSecret) {
        this.restClient = RestClient.builder().baseUrl(internalUrl).build();
        this.internalSecret = internalSecret;
    }

    /** Best-effort: disconnect the user's live game-server-java session, if any. */
    public void kickUser(UUID userId, String reason) {
        try {
            restClient.post()
                    .uri("/internal/users/{id}/kick", userId)
                    .header("X-Internal-Secret", internalSecret)
                    .body(new KickBody(reason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to notify game-server-java of ban-kick for user {} (non-fatal): {}",
                    userId, e.getMessage());
        }
    }

    private record KickBody(String reason) {}
}
