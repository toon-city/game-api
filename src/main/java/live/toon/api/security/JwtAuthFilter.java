package live.toon.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.toon.api.repository.UserRepository;
import live.toon.api.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre JWT inscrit UNIQUEMENT dans la chaîne Spring Security (via SecurityConfig).
 * Ne porte PAS @Component pour éviter la double-enregistrement comme servlet filter.
 *
 * À chaque requête avec un token valide :
 *   1. Valide la signature et l'expiration via JwtService
 *   2. Extrait l'userId depuis le claim "sub"
 *   3. Charge l'objet User depuis la DB (rang, statut frais)
 *   4. Construit un JwtPrincipal avec les données DB et ses GrantedAuthorities
 *
 * Le token JWT n'est utilisé QUE pour identifier l'utilisateur (sub = userId).
 * Le rank et le username proviennent TOUJOURS de la DB.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService     jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService     = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtService.isValid(token)) {
                UUID userId = jwtService.extractUserId(token);
                // Charge les données fraîches depuis la DB (rang, statut, username actuel)
                userRepository.findById(userId).ifPresent(user -> {
                    UserRank  rank      = UserRank.fromRank(user.getRank());
                    var       principal = new JwtPrincipal(user.getId(), user.getUsername(), rank);
                    var       auth      = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        } catch (Exception e) {
            // Token invalide ou erreur imprévue : la requête continue sans auth.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
