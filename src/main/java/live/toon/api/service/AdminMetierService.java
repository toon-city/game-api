package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.AdminMetierRequest;
import live.toon.api.dto.MetierDto;
import live.toon.api.entity.Metier;
import live.toon.api.repository.MetierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin CRUD for the métier catalogue — same shape as AdminItemService (small catalogue, no paging needed). */
@Service
@RequiredArgsConstructor
public class AdminMetierService {

    private final MetierRepository metierRepository;

    @Transactional(readOnly = true)
    public List<MetierDto> listMetiers() {
        return metierRepository.findAll(Sort.by("name").ascending()).stream().map(this::toDto).toList();
    }

    @Transactional
    public MetierDto createMetier(AdminMetierRequest req) {
        Metier metier = Metier.builder()
                .name(req.name())
                .dailyPez(req.dailyPez())
                .minToonizLevel(req.minToonizLevel())
                .minDaysPlayed(req.minDaysPlayed())
                .build();
        return toDto(metierRepository.save(metier));
    }

    @Transactional
    public MetierDto updateMetier(Long id, AdminMetierRequest req) {
        Metier metier = requireMetier(id);
        if (req.name() != null) metier.setName(req.name());
        metier.setDailyPez(req.dailyPez());
        metier.setMinToonizLevel(req.minToonizLevel());
        metier.setMinDaysPlayed(req.minDaysPlayed());
        return toDto(metierRepository.save(metier));
    }

    @Transactional
    public void deleteMetier(Long id) {
        // Users pointing at this métier fall back to null via ON DELETE SET NULL
        // (see V22) — no orphan FK, no need to touch User rows here.
        metierRepository.delete(requireMetier(id));
    }

    private Metier requireMetier(Long id) {
        return metierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Métier introuvable : " + id));
    }

    private MetierDto toDto(Metier m) {
        return MetierDto.builder()
                .id(m.getId())
                .name(m.getName())
                .dailyPez(m.getDailyPez())
                .minToonizLevel(m.getMinToonizLevel())
                .minDaysPlayed(m.getMinDaysPlayed())
                .build();
    }
}
