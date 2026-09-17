package live.toon.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "house_schemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "house_data", columnDefinition = "TEXT", nullable = false)
    private String houseData;

    /** False for shapes reserved for a specific public room (Jardin, Disco,
     *  Quizz, Maison hantée) — excluded from the picker a player sees when
     *  creating their own private house. */
    @Column(nullable = false)
    @Builder.Default
    private boolean selectable = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
