package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.HouseDto;
import live.toon.api.dto.HouseRequest;
import live.toon.api.dto.HouseSchemaDto;
import live.toon.api.entity.*;
import live.toon.api.repository.HouseSchemaRepository;
import live.toon.api.repository.RoomRepository;
import live.toon.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    public List<HouseDto> listPrivateHouses() {
        return roomRepository.findAllByType(RoomType.PRIVATE).stream()
                .map(this::toHouseDto)
                .toList();
    }

    @Transactional
    public HouseDto updateHouse(UUID requesterId, Long houseId, HouseRequest req) {
        Room room = findPrivateAndCheckOwner(requesterId, houseId);

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

    @Transactional
    public void deleteHouse(UUID requesterId, Long houseId) {
        Room room = findPrivateAndCheckOwner(requesterId, houseId);
        roomRepository.delete(room);
    }

    /**
     * Valide l'accès d'un utilisateur à une maison privée.
     * Retourne silencieusement si OK, lève une exception sinon.
     */
    public void validateAccess(UUID requesterId, Long houseId, String providedPassword) {
        Room room = roomRepository.findById(houseId)
                .orElseThrow(() -> new EntityNotFoundException("Maison introuvable : " + houseId));

        if (room.getType() != RoomType.PRIVATE) {
            return; // Les salles publiques sont toujours accessibles
        }

        // Le propriétaire peut toujours entrer
        boolean isOwner = room.getOwner() != null &&
                room.getOwner().getId().equals(requesterId);
        if (isOwner) return;

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

    private Room findPrivateAndCheckOwner(UUID requesterId, Long houseId) {
        Room room = roomRepository.findById(houseId)
                .orElseThrow(() -> new EntityNotFoundException("Maison introuvable : " + houseId));
        if (room.getType() != RoomType.PRIVATE) {
            throw new IllegalArgumentException("Cette salle n'est pas une maison privée");
        }
        if (room.getOwner() == null || !room.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Vous n'êtes pas le propriétaire de cette maison");
        }
        return room;
    }

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
                .build();
    }
}
