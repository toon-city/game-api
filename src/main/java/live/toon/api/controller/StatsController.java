package live.toon.api.controller;

import live.toon.api.dto.StatsDto;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository userRepository;

    /** Retourne les stats publiques du jeu (joueurs inscrits, connectés). */
    @GetMapping
    public StatsDto getStats() {
        return StatsDto.builder()
                .registeredCount(userRepository.count())
                // onlineCount sera alimenté lors de l'intégration game-server
                .onlineCount(0)
                .build();
    }
}
