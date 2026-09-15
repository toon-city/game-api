package live.toon.api.service;

import live.toon.api.entity.User;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks "days played" and pays out the daily métier income — called from
 * JwtAuthFilter on every authenticated request (not just login), matching
 * "dès que l'utilisateur se connecte/joue (requête) sur un nouveau jour".
 * Re-fetches the user itself rather than reusing the filter's own lookup:
 * that one is done outside a transaction and gets detached immediately,
 * so touching the lazy `metier` association on it would throw
 * LazyInitializationException.
 */
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;

    @Transactional
    public void touchDailyActivity(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastPlayedDate();

        if (last == null) {
            // First request ever seen for this account (pre-existing user from
            // before this column existed) — just start tracking from today,
            // the column's own default(1) already counts today as day one.
            user.setLastPlayedDate(today);
            userRepository.save(user);
            return;
        }

        if (!last.isEqual(today)) {
            user.setDaysPlayed(user.getDaysPlayed() + 1);
            user.setLastPlayedDate(today);
            if (user.getMetier() != null) {
                user.setPez(user.getPez() + user.getMetier().getDailyPez());
            }
            userRepository.save(user);
        }
    }
}
