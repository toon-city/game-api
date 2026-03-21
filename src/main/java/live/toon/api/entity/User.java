package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(unique = true, length = 255)
    private String email;

    /** 0 = user, 1 = moderator, 2 = admin */
    @Column(nullable = false)
    @Builder.Default
    private int rank = 0;

    /** Tooniz badge level: 0 = none, 1/2/3 */
    @Column(name = "tooniz_level", nullable = false)
    @Builder.Default
    private int toonizLevel = 0;

    /** Monnaie payante (achat en boutique) */
    @Column(nullable = false)
    @Builder.Default
    private int kreds = 0;

    /** Monnaie gratuite (gagnée en jouant) */
    @Column(nullable = false)
    @Builder.Default
    private int pez = 1500;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;
}
