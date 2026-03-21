package live.toon.api.service;

import live.toon.api.dto.DeditoonDto;
import live.toon.api.entity.Deditoon;
import live.toon.api.entity.User;
import live.toon.api.repository.DeditoonRepository;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeditoonService {

    private static final int DEDITOON_COST = 10;

    private final DeditoonRepository deditoonRepository;
    private final UserRepository userRepository;

    /** Retourne les 10 dernières déditoons. */
    @Transactional(readOnly = true)
    public List<DeditoonDto> getLatest() {
        return deditoonRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Publie une déditoon (déduit 10 kreds de l'auteur). */
    @Transactional
    public DeditoonDto post(UUID authorId, String message) {
        User user = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (user.getKreds() < DEDITOON_COST) {
            throw new IllegalArgumentException(
                    "Kreds insuffisants (il vous faut au moins " + DEDITOON_COST + " kreds)");
        }

        user.setKreds(user.getKreds() - DEDITOON_COST);
        userRepository.save(user);

        Deditoon deditoon = deditoonRepository.save(
                Deditoon.builder()
                        .author(user)
                        .message(message)
                        .build());

        return toDto(deditoon);
    }

    private DeditoonDto toDto(Deditoon d) {
        return DeditoonDto.builder()
                .id(d.getId())
                .authorUsername(d.getAuthor().getUsername())
                .authorGender(d.getAuthor().getGender() != null ? d.getAuthor().getGender().name() : null)
                .message(d.getMessage())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
