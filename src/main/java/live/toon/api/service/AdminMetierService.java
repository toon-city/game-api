package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.AdminMetierRequest;
import live.toon.api.dto.MetierDto;
import live.toon.api.entity.Item;
import live.toon.api.entity.Metier;
import live.toon.api.repository.ItemRepository;
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
    private final ItemRepository itemRepository;

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
                .outfitTshirt(resolveItem(req.outfitTshirtItemId()))
                .outfitPant(resolveItem(req.outfitPantItemId()))
                .outfitHat(resolveItem(req.outfitHatItemId()))
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
        metier.setOutfitTshirt(resolveItem(req.outfitTshirtItemId()));
        metier.setOutfitPant(resolveItem(req.outfitPantItemId()));
        metier.setOutfitHat(resolveItem(req.outfitHatItemId()));
        return toDto(metierRepository.save(metier));
    }

    @Transactional
    public void deleteMetier(Long id) {
        // Users pointing at this métier fall back to null via ON DELETE SET NULL
        // (see V22) — no orphan FK, no need to touch User rows here.
        metierRepository.delete(requireMetier(id));
    }

    private Item resolveItem(Long itemId) {
        if (itemId == null) return null;
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable : " + itemId));
    }

    private Metier requireMetier(Long id) {
        return metierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Métier introuvable : " + id));
    }

    private MetierDto toDto(Metier m) {
        Item tshirt = m.getOutfitTshirt();
        Item pant = m.getOutfitPant();
        Item hat = m.getOutfitHat();
        return MetierDto.builder()
                .id(m.getId())
                .name(m.getName())
                .dailyPez(m.getDailyPez())
                .minToonizLevel(m.getMinToonizLevel())
                .minDaysPlayed(m.getMinDaysPlayed())
                .outfitTshirtItemId(tshirt != null ? tshirt.getId() : null)
                .outfitTshirtItemName(tshirt != null ? tshirt.getName() : null)
                .outfitPantItemId(pant != null ? pant.getId() : null)
                .outfitPantItemName(pant != null ? pant.getName() : null)
                .outfitHatItemId(hat != null ? hat.getId() : null)
                .outfitHatItemName(hat != null ? hat.getName() : null)
                .build();
    }
}
