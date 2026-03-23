package live.toon.api.config;

import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtAuthFilter;
import live.toon.api.security.ResourcePermissionEvaluator;
import live.toon.api.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService     jwtService;
    private final UserRepository userRepository;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtService, userRepository);
    }

    /**
     * Enregistre le {@link ResourcePermissionEvaluator} pour que les expressions
     * {@code hasPermission()} dans {@code @PreAuthorize} soient correctement déléguées.
     * Déclaré {@code static} pour éviter tout cycle de dépendance lors de l'init du contexte.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            ResourcePermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // === PUBLIC — accessible sans token ===
                .requestMatchers("/api/auth/register", "/api/auth/token").permitAll()
                .requestMatchers("/api/stats").permitAll()
                .requestMatchers("/api/house-schemas").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/rooms", "/api/rooms/paged", "/api/rooms/*").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/houses", "/api/houses/paged").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/deditoons").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/shops/*/items").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/shops/*/collections").permitAll()
                .requestMatchers("/error").permitAll()

                // === AUTHENTICATED — token requis, autorisation fine via @PreAuthorize ===
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // 401 — token absent ou expiré (non authentifié)
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write("{\"status\":401,\"error\":\"Non authentifi\u00e9\"}");
                })
                // 403 — authentifié mais accès refusé (ressource d'autrui)
                .accessDeniedHandler((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.getWriter().write("{\"status\":403,\"error\":\"Acc\u00e8s refus\u00e9\"}");
                })
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
