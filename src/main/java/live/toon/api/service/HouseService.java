package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.HouseDto;
import live.toon.api.dto.HouseRequest;
import live.toon.api.dto.HouseSchemaDto;
import live.toon.api.entity.*;
import live.toon.api.repository.HouseSchemaRepository;
import live.toon.api.repository.RoomRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.policy.HousePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final RoomRepository roomRepository;
    private final HouseSchemaRepository schemaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HousePolicy housePolicy;

    // ── Schémas ───────────────────────────────────────────────────────────────

    public List<HouseSchemaDto> listSchemas() {
        return schemaRepository.findAll().stream()
                .map(s -> HouseSchemaDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .build())
                .toList();
    }

    // ── Maisons privées ───────────────────────────────────────────────────────

    @Transactional
    public HouseDto createHouse(UUID ownerId, HouseRequest req) {
        if (req.getSchemaId() == null) {
            throw new IllegalArgumentException("Un schéma de maison est obligatoire");
        }
        if (req.getAccess() == HouseAccess.PASSWORD &&
                (req.getPassword() == null || req.getPassword().isBlank())) {
            throw new IllegalArgumentException("Un mot de passe est requis pour ce mode d'accès");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        HouseSchema schema = schemaRepository.findById(req.getSchemaId())
                .orElseThrow(() -> new EntityNotFoundException("Schéma introuvable : " + req.getSchemaId()));

        String hash = null;
        if (req.getAccess() == HouseAccess.PASSWORD) {
            hash = passwordEncoder.encode(req.getPassword());
        }

        Room room = Room.builder()
                .name(req.getName())
                .type(RoomType.PRIVATE)
                .access(req.getAccess())
                .owner(owner)
                .passwordHash(hash)
                .schema(schema)
                .houseData(schema.getHouseData())
                .maxUsers(20)
                .build();

        return toHouseDto(roomRepository.save(room));
    }

    public List<HouseDto> listMyHouses(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        return roomRepository.findAllByOwner(owner).stream()
                .map(this::toHouseDto)
                .toList();
    }

    /** Liste paginée des maisons d'un utilisateur, triée par userCount DESC puis name ASC. */
    @Transactional(readOnly = true)
    public Page<HouseDto> listMyHousesPaged(UUID ownerId, String search, int page, int size) {
        String q = search == null ? "" : search.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return roomRepository.findMyHousesPaged(ownerId, q, pageable).map(this::toHouseDto);
    }

    public List<HouseDto> listPrivateHouses() {
        return roomRepository.findAllByType(RoomType.PRIVATE).stream()
                .map(this::toHouseDto)
                .toList();
    }

    /**
     * Returns a page of private houses sorted by userCount DESC then name ASC.
     * When {@code search} is blank, only houses with users present are returned.
     * Searches by house name OR owner username.
     */
    @Transactional(readOnly = true)
    public Page<HouseDto> listPrivateHousesPaged(String search, int page, int size) {
        boolean peopleOnly = search == null || search.isBlank();
        String q = search == null ? "" : search.trim();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, Math.min(size, 50));
        return roomRepository
                .findHousesFiltered(q, peopleOnly, pageable)
                .map(this::toHouseDto);
    }

    /**
     * Modifie une maison existante.
     * L'autorisation (propriétaire ou admin) est vérifiée en amont par
     * {@code @PreAuthorize("hasPermission(#id, 'House', 'update')")} dans le controller.
     */
    @Transactional
    public HouseDto updateHouse(Long houseId, HouseRequest req) {
        Room room = roomRepository.findById(houseId)
                .orElseThrow(() -> new EntityNotFoundException("Maison introuvable : " + houseId));
        if (room.getType() != RoomType.PRIVATE) {
            throw new IllegalArgumentException("Cette salle n'est pas une maison privée");
        }

        if (req.getAccess() == HouseAccess.PASSWORD &&
                (req.getPassword() == null || req.getPassword().isBlank())) {
            throw new IllegalArgumentException("Un mot de passe est requis pour ce mode d'accès");
        }

        room.setName(req.getName());
        room.setAccess(req.getAccess());

        if (req.getAccess() == HouseAccess.PASSWORD) {
            room.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        } else {
            room.setPasswordHash(null);
        }

        if (req.getSchemaId() != null && !req.getSchemaId().equals(
                room.getSchema() != null ? room.getSchema().getId() : null)) {
            HouseSchema schema = schemaRepository.findById(req.getSchemaId())
                    .orElseThrow(() -> new EntityNotFoundException("Schéma introuvable : " + req.getSchemaId()));
            room.setSchema(schema);
            room.setHouseData(schema.getHouseData());
        }

        return toHouseDto(roomRepository.save(room));
    }

    /**
     * Supprime une maison.
     * L'autorisation est vérifiée en amont par
     * {@code @PreAuthorize("hasPermission(#id, 'House', 'delete')")} dans le controller.
     */
    @Transactional
    public void deleteHouse(Long houseId) {
        Room room = roomRepository.findById(houseId)
                .orElseThrow(() -> new EntityNotFoundException("Maison introuvable : " + houseId));
        roomRepository.delete(room);
    }

    /**
     * Valide l'accès d'un utilisateur à une maison privée avant de rejoindre la salle.
     * Les modérateurs et admins passent toujours (canBypassRestrictions).
     */
    public void validateAccess(JwtPrincipal actor, Long houseId, String providedPassword) {
        Room room = roomRepository.findById(houseId)
                .orElseThrow(() -> new EntityNotFoundException("Maison introuvable : " + houseId));

        if (room.getType() != RoomType.PRIVATE) {
            return; // Les salles publiques sont toujours accessibles
        }

        // Propriétaire, modérateurs et admins entrent sans restriction
        if (housePolicy.canBypassRestrictions(actor, room)) return;

        switch (room.getAccess()) {
            case OPEN -> { /* ok */ }
            case CLOSED -> throw new IllegalArgumentException("Cette maison est fermée");
            case PASSWORD -> {
                if (providedPassword == null || providedPassword.isBlank()) {
                    throw new IllegalArgumentException("Un mot de passe est requis");
                }
                if (!passwordEncoder.matches(providedPassword, room.getPasswordHash())) {
                    throw new IllegalArgumentException("Mot de passe incorrect");
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────


    private HouseDto toHouseDto(Room room) {
        return HouseDto.builder()
                .id(room.getId())
                .name(room.getName())
                .type(room.getType().name())
                .access(room.getAccess().name())
                .ownerUsername(room.getOwner() != null ? room.getOwner().getUsername() : null)
                .hasPassword(room.getPasswordHash() != null)
                .schemaId(room.getSchema() != null ? room.getSchema().getId() : null)
                .schemaName(room.getSchema() != null ? room.getSchema().getName() : null)
                .maxUsers(room.getMaxUsers())
                .userCount(room.getUserCount())
                .build();
    }
}
