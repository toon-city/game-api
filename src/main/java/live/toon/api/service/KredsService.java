package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.KredsPackageDto;
import live.toon.api.entity.KredsPackage;
import live.toon.api.entity.KredsPurchase;
import live.toon.api.entity.User;
import live.toon.api.repository.KredsPackageRepository;
import live.toon.api.repository.KredsPurchaseRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KredsService {

    private final KredsPackageRepository kredsPackageRepository;
    private final KredsPurchaseRepository kredsPurchaseRepository;
    private final UserRepository userRepository;

    // ─── Catalogue (public) ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<KredsPackageDto> listActivePackages() {
        return kredsPackageRepository.findByActiveTrueOrderByPriceCentsAsc()
                .stream().map(this::toDto).toList();
    }

    // ─── Achat (mock — pas de paiement réel) ─────────────────────────────────

    @Transactional
    public KredsPackageDto purchasePackage(JwtPrincipal actor, Long packageId) {
        KredsPackage pkg = kredsPackageRepository.findById(packageId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Package kreds introuvable : " + packageId));
        if (!pkg.isActive()) throw new IllegalStateException("Ce package n'est plus disponible");

        User user = userRepository.findById(actor.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        user.setKreds(user.getKreds() + pkg.getKredsAmount());
        userRepository.save(user);

        KredsPurchase purchase = KredsPurchase.builder()
                .user(user)
                .kredsPackage(pkg)
                .kredsAmount(pkg.getKredsAmount())
                .priceCents(pkg.getPriceCents())
                .build();
        kredsPurchaseRepository.save(purchase);

        return toDto(pkg);
    }

    // ─── Admin CRUD packages ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<KredsPackageDto> listAllPackages(int page) {
        return kredsPackageRepository.findAll(
                PageRequest.of(page, 20, Sort.by("priceCents").ascending()))
                .map(this::toDto);
    }

    @Transactional
    public KredsPackageDto createPackage(KredsPackageDto req) {
        KredsPackage pkg = KredsPackage.builder()
                .name(req.getName() != null ? req.getName() : "")
                .kredsAmount(req.getKredsAmount())
                .priceCents(req.getPriceCents())
                .currency(req.getCurrency() != null ? req.getCurrency() : "EUR")
                .active(req.getActive() != null ? req.getActive() : true)
                .build();
        return toDto(kredsPackageRepository.save(pkg));
    }

    @Transactional
    public KredsPackageDto updatePackage(Long id, KredsPackageDto req) {
        KredsPackage pkg = kredsPackageRepository.findById(id.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Package kreds introuvable : " + id));
        if (req.getName() != null)        pkg.setName(req.getName());
        if (req.getKredsAmount() != null) pkg.setKredsAmount(req.getKredsAmount());
        if (req.getPriceCents() != null)  pkg.setPriceCents(req.getPriceCents());
        if (req.getCurrency() != null)    pkg.setCurrency(req.getCurrency());
        if (req.getActive() != null)      pkg.setActive(req.getActive());
        return toDto(kredsPackageRepository.save(pkg));
    }

    @Transactional
    public void deletePackage(Long id) {
        kredsPackageRepository.deleteById(id.intValue());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private KredsPackageDto toDto(KredsPackage p) {
        return KredsPackageDto.builder()
                .id(p.getId())
                .name(p.getName())
                .kredsAmount(p.getKredsAmount())
                .priceCents(p.getPriceCents())
                .currency(p.getCurrency())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
